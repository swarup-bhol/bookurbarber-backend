package com.trimly.service;

import com.trimly.dto.*;
import com.trimly.entity.*;
import com.trimly.enums.BookingStatus;
import com.trimly.enums.RescheduleStatus;
import com.trimly.enums.ShopStatus;
import com.trimly.exception.TrimlyException;
import com.trimly.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository       bookingRepo;
    private final ShopRepository          shopRepo;
    private final BarberServiceRepository svcRepo;
    private final UserRepository          userRepo;
    private final EmployeeRepository      empRepo;
    private final SlotBlockRepository     blockRepo;
    private final EmployeeService         employeeService;
    private final WhatsAppService         wa;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    // ── Customer — Create ─────────────────────────────────────────────────

    @Transactional
    public BookingResponse create(Long customerId, BookingRequest req) {
        User customer = userRepo.findById(customerId)
            .orElseThrow(() -> TrimlyException.notFound("User not found"));
        Shop shop = shopRepo.findById(req.getShopId())
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));

        if (shop.getStatus() != ShopStatus.ACTIVE)
            throw TrimlyException.badRequest("Shop is not currently accepting bookings");
        if (!shop.isOpen())
            throw TrimlyException.badRequest("Shop is currently closed");

        // Validate services
        List<BarberService> svcs = svcRepo.findAllById(req.getServiceIds());
        if (svcs.size() != req.getServiceIds().size())
            throw TrimlyException.badRequest("One or more selected services not found");
        if (svcs.stream().anyMatch(s -> !s.getShop().getId().equals(shop.getId()) || !s.isEnabled()))
            throw TrimlyException.badRequest("Selected services are not available at this shop");

        // Seat-aware availability
        int seatsUsed = bookingRepo.countSeatsUsedAtSlot(shop.getId(), req.getBookingDate(), req.getSlotTime());
        if (seatsUsed + req.getSeats() > shop.getSeats())
            throw TrimlyException.conflict("Not enough seats at this time slot. Please pick another.");

        // Check shop-wide slot block
        if (blockRepo.isSlotBlockedShopWide(shop.getId(), req.getBookingDate(), req.getSlotTime()))
            throw TrimlyException.conflict("This time slot is not available. Please choose another.");

        // Resolve optional employee preference
        Employee employee = null;
        if (req.getEmployeeId() != null) {
            employee = empRepo.findById(req.getEmployeeId())
                .orElseThrow(() -> TrimlyException.notFound("Employee not found"));
            if (!employee.getShop().getId().equals(shop.getId()))
                throw TrimlyException.badRequest("Employee does not belong to this shop");
            if (!employee.isActive())
                throw TrimlyException.badRequest("This employee is not available today");
            // Check employee-specific slot block
            if (blockRepo.isSlotBlockedForEmployee(
                    shop.getId(), req.getBookingDate(), req.getSlotTime(), employee.getId()))
                throw TrimlyException.conflict(
                    "This employee is not available at that time. Please choose another slot or employee.");
        }

        // Financials
        int duration      = svcs.stream().mapToInt(BarberService::getDurationMinutes).sum();
        BigDecimal total  = svcs.stream().map(BarberService::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fee    = total.multiply(shop.getCommissionPercent())
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        String snapshot   = svcs.stream().map(BarberService::getServiceName).collect(Collectors.joining(", "));
        String ids        = req.getServiceIds().stream().map(String::valueOf).collect(Collectors.joining(","));

        Booking b = bookingRepo.save(Booking.builder()
            .shop(shop).customer(customer)
            .employee(employee)
            .employeeSnapshot(employee != null ? employee.getName() : null)
            .servicesSnapshot(snapshot).serviceIds(ids)
            .bookingDate(req.getBookingDate()).slotTime(req.getSlotTime())
            .durationMinutes(duration).seats(req.getSeats())
            .totalAmount(total).platformFee(fee).barberEarning(total.subtract(fee))
            .build());

        // WhatsApp notify barber
        String barberPhone = shop.getOwner().getPhone();
        String date = req.getBookingDate().format(DATE_FMT);
        String time = req.getSlotTime().format(TIME_FMT);
        String ref  = "#TRM" + b.getId();
        String empNote = employee != null ? " (for " + employee.getName() + ")" : "";
        wa.sendBookingRequestToBarber(barberPhone, customer.getFullName(), snapshot + empNote, date, time, ref);

        return toResp(b, false);
    }

    // ── Barber — List & Stats ─────────────────────────────────────────────

    public List<BookingResponse> getBarberBookings(Long ownerId, BookingStatus status) {
        Shop shop = shopRepo.findByOwner_Id(ownerId)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));
        List<Booking> list = status != null
            ? bookingRepo.findByShop_IdAndStatusOrderByCreatedAtDesc(shop.getId(), status)
            : bookingRepo.findByShop_IdOrderByCreatedAtDesc(shop.getId());
        return list.stream().map(b -> toResp(b, true)).collect(Collectors.toList());
    }

    public DashboardStats getBarberStats(Long ownerId) {
        Shop shop = shopRepo.findByOwner_Id(ownerId)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));
        long total     = bookingRepo.countByShop_Id(shop.getId());
        long pending   = bookingRepo.countByShop_IdAndStatus(shop.getId(), BookingStatus.PENDING);
        long confirmed = bookingRepo.countByShop_IdAndStatus(shop.getId(), BookingStatus.CONFIRMED);
        long completed = bookingRepo.countByShop_IdAndStatus(shop.getId(), BookingStatus.COMPLETED);
        BigDecimal rev  = bookingRepo.totalRevenueByShop(shop.getId());
        BigDecimal comm = rev.multiply(shop.getCommissionPercent())
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return DashboardStats.builder()
            .totalBookings(total).pendingBookings(pending)
            .confirmedBookings(confirmed).completedBookings(completed)
            .totalRevenue(rev).totalCommission(comm)
            .barberEarnings(rev.subtract(comm)).build();
    }

    // ── Barber — Accept ───────────────────────────────────────────────────

    @Transactional
    public BookingResponse accept(Long ownerId, Long id) {
        Booking b = barberBooking(ownerId, id);
        if (b.getStatus() != BookingStatus.PENDING)
            throw TrimlyException.badRequest("Only pending bookings can be accepted");
        b.setStatus(BookingStatus.CONFIRMED);
        bookingRepo.save(b);

        String date = b.getBookingDate().format(DATE_FMT);
        String time = b.getSlotTime().format(TIME_FMT);
        String empNote = b.getEmployee() != null ? " Your barber: " + b.getEmployee().getName() + "." : "";
        wa.sendBookingConfirmedToCustomer(
            b.getCustomer().getPhone(), b.getCustomer().getFullName(),
            b.getShop().getShopName(), b.getServicesSnapshot() + empNote, date, time);

        return toResp(b, true);
    }

    // ── Barber — Reject ───────────────────────────────────────────────────

    @Transactional
    public BookingResponse reject(Long ownerId, Long id, BookingActionRequest req) {
        Booking b = barberBooking(ownerId, id);
        if (b.getStatus() != BookingStatus.PENDING)
            throw TrimlyException.badRequest("Only pending bookings can be rejected");
        b.setStatus(BookingStatus.REJECTED);
        b.setCancelReason(req.getCancelReason());
        bookingRepo.save(b);

        wa.sendBookingRejectedToCustomer(
            b.getCustomer().getPhone(), b.getCustomer().getFullName(),
            b.getShop().getShopName(), b.getServicesSnapshot(), req.getCancelReason());

        return toResp(b, true);
    }

    // ── Barber — Cancel ───────────────────────────────────────────────────

    @Transactional
    public BookingResponse cancelByBarber(Long ownerId, Long id, BookingActionRequest req) {
        Booking b = barberBooking(ownerId, id);
        if (b.getStatus() != BookingStatus.CONFIRMED)
            throw TrimlyException.badRequest("Only confirmed bookings can be cancelled by the barber");
        b.setStatus(BookingStatus.CANCELLED);
        b.setCancelReason(req.getCancelReason());
        bookingRepo.save(b);

        String date = b.getBookingDate().format(DATE_FMT);
        String time = b.getSlotTime().format(TIME_FMT);
        wa.sendCancellationNotice(
            b.getCustomer().getPhone(), b.getCustomer().getFullName(),
            b.getShop().getShopName(), date, time);

        return toResp(b, true);
    }

    // ── Barber — Complete ─────────────────────────────────────────────────

    @Transactional
    public BookingResponse complete(Long ownerId, Long id) {
        Booking b = barberBooking(ownerId, id);
        if (b.getStatus() != BookingStatus.CONFIRMED)
            throw TrimlyException.badRequest("Only confirmed bookings can be completed");
        b.setStatus(BookingStatus.COMPLETED);

        // Update shop totals
        Shop shop = b.getShop();
        shop.setTotalBookings(shop.getTotalBookings() + 1);
        shop.setMonthlyRevenue(shop.getMonthlyRevenue().add(b.getTotalAmount()));
        shopRepo.save(shop);

        // Update employee totals
        if (b.getEmployee() != null) {
            Employee emp = b.getEmployee();
            emp.setTotalBookings(emp.getTotalBookings() + 1);
            emp.setTotalEarnings(emp.getTotalEarnings().add(b.getBarberEarning()));
            empRepo.save(emp);
        }

        bookingRepo.save(b);
        wa.sendBookingCompleted(
            b.getCustomer().getPhone(), b.getCustomer().getFullName(),
            shop.getShopName(), "₹" + b.getTotalAmount());

        return toResp(b, true);
    }

    // ── Barber — Reschedule ───────────────────────────────────────────────

    @Transactional
    public BookingResponse requestReschedule(Long ownerId, Long id, RescheduleRequest req) {
        Booking b = barberBooking(ownerId, id);
        if (b.getStatus() != BookingStatus.CONFIRMED && b.getStatus() != BookingStatus.PENDING)
            throw TrimlyException.badRequest("Can only reschedule pending or confirmed bookings");

        int newSeatsUsed = bookingRepo.countSeatsUsedAtSlot(
            b.getShop().getId(), req.getNewDate(), req.getNewTime());
        if (newSeatsUsed + b.getSeats() > b.getShop().getSeats())
            throw TrimlyException.conflict("The new slot doesn't have enough seats available");

        String oldTime = b.getSlotTime().format(TIME_FMT);
        b.setRescheduleDate(req.getNewDate());
        b.setRescheduleTime(req.getNewTime());
        b.setRescheduleReason(req.getReason());
        b.setRescheduleStatus(RescheduleStatus.PENDING);
        b.setStatus(BookingStatus.RESCHEDULE_REQUESTED);
        bookingRepo.save(b);

        String newDate = req.getNewDate().format(DATE_FMT);
        String newTime = req.getNewTime().format(TIME_FMT);
        String empNote = b.getEmployee() != null ? " Your barber: " + b.getEmployee().getName() + "." : "";
        wa.sendRescheduleRequestToCustomer(
            b.getCustomer().getPhone(), b.getCustomer().getFullName(),
            b.getShop().getShopName() + empNote, oldTime, newDate, newTime, req.getReason());

        return toResp(b, true);
    }

    // ── Customer — Respond to Reschedule ──────────────────────────────────

    @Transactional
    public BookingResponse respondToReschedule(Long customerId, Long id, RescheduleResponseRequest req) {
        Booking b = bookingRepo.findById(id)
            .orElseThrow(() -> TrimlyException.notFound("Booking not found"));
        if (!b.getCustomer().getId().equals(customerId))
            throw TrimlyException.forbidden("Not your booking");
        if (b.getStatus() != BookingStatus.RESCHEDULE_REQUESTED)
            throw TrimlyException.badRequest("No pending reschedule request for this booking");

        String barberPhone = b.getShop().getOwner().getPhone();
        String newTime     = b.getRescheduleTime().format(TIME_FMT);

        if (req.isAccept()) {
            b.setBookingDate(b.getRescheduleDate());
            b.setSlotTime(b.getRescheduleTime());
            b.setRescheduleStatus(RescheduleStatus.ACCEPTED);
            b.setStatus(BookingStatus.CONFIRMED);
            wa.sendRescheduleResponseToBarber(barberPhone, b.getShop().getShopName(),
                b.getCustomer().getFullName(), newTime, "Accepted ✅");
        } else {
            b.setRescheduleStatus(RescheduleStatus.DECLINED);
            b.setStatus(BookingStatus.CONFIRMED);
            wa.sendRescheduleResponseToBarber(barberPhone, b.getShop().getShopName(),
                b.getCustomer().getFullName(), newTime, "Declined ❌");
        }

        b.setRescheduleDate(null);
        b.setRescheduleTime(null);
        b.setRescheduleReason(null);
        bookingRepo.save(b);
        return toResp(b, false);
    }

    // ── Customer — Cancel ─────────────────────────────────────────────────

    @Transactional
    public BookingResponse cancelByCustomer(Long customerId, Long id) {
        Booking b = bookingRepo.findById(id)
            .orElseThrow(() -> TrimlyException.notFound("Booking not found"));
        if (!b.getCustomer().getId().equals(customerId))
            throw TrimlyException.forbidden("Not your booking");
        if (b.getStatus() != BookingStatus.PENDING
                && b.getStatus() != BookingStatus.CONFIRMED
                && b.getStatus() != BookingStatus.RESCHEDULE_REQUESTED)
            throw TrimlyException.badRequest("Cannot cancel at this stage");

        b.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(b);

        String date = b.getBookingDate().format(DATE_FMT);
        String time = b.getSlotTime().format(TIME_FMT);
        wa.sendCancellationNotice(
            b.getShop().getOwner().getPhone(), b.getShop().getShopName(),
            b.getCustomer().getFullName(), date, time);

        return toResp(b, false);
    }

    // ── Customer — Rate ───────────────────────────────────────────────────

    @Transactional
    public BookingResponse rate(Long customerId, Long id, RatingRequest req) {
        Booking b = bookingRepo.findById(id)
            .orElseThrow(() -> TrimlyException.notFound("Booking not found"));
        if (!b.getCustomer().getId().equals(customerId))
            throw TrimlyException.forbidden("Not your booking");
        if (b.getStatus() != BookingStatus.COMPLETED)
            throw TrimlyException.badRequest("Only completed bookings can be rated");
        if (b.getRating() != null)
            throw TrimlyException.badRequest("You have already rated this booking");

        b.setRating(req.getRating());
        b.setReview(req.getReview());

        // Employee rating (optional)
        if (req.getEmployeeRating() != null && b.getEmployee() != null) {
            b.setEmployeeRating(req.getEmployeeRating());
        }
        bookingRepo.save(b);

        // Recalculate shop avg rating
        Shop shop = b.getShop();
        List<Booking> rated = bookingRepo.findByShop_IdOrderByCreatedAtDesc(shop.getId())
            .stream().filter(bk -> bk.getRating() != null).toList();
        if (!rated.isEmpty()) {
            double avg = rated.stream().mapToInt(Booking::getRating).average().orElse(0);
            shop.setAvgRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            shop.setTotalReviews(rated.size());
            shopRepo.save(shop);
        }

        // Recalculate employee avg rating
        if (b.getEmployee() != null && b.getEmployeeRating() != null) {
            employeeService.updateEmployeeRating(b.getEmployee().getId());
        }

        return toResp(b, false);
    }

    // ── Customer — List ───────────────────────────────────────────────────

    public List<BookingResponse> getCustomerBookings(Long customerId) {
        return bookingRepo.findByCustomer_IdOrderByCreatedAtDesc(customerId)
            .stream().map(b -> toResp(b, false)).collect(Collectors.toList());
    }

    // ── Admin ─────────────────────────────────────────────────────────────

    public List<BookingResponse> getAllAdmin(BookingStatus status) {
        List<Booking> list = status != null
            ? bookingRepo.findByStatusOrderByCreatedAtDesc(status)
            : bookingRepo.findAllByOrderByCreatedAtDesc();
        return list.stream().map(b -> toResp(b, true)).collect(Collectors.toList());
    }

    public DashboardStats getAdminStats() {
        return DashboardStats.builder()
            .totalShops(shopRepo.count())
            .activeShops(shopRepo.countByStatus(com.trimly.enums.ShopStatus.ACTIVE))
            .pendingShops(shopRepo.countByStatus(com.trimly.enums.ShopStatus.PENDING))
            .totalBookings(bookingRepo.count())
            .pendingBookings(bookingRepo.countByStatus(BookingStatus.PENDING))
            .totalCommission(bookingRepo.totalPlatformCommission())
            .totalRevenue(bookingRepo.findAllByOrderByCreatedAtDesc().stream()
                .filter(bk -> bk.getStatus() == BookingStatus.COMPLETED)
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .totalCustomers(userRepo.countByRole(com.trimly.enums.Role.CUSTOMER))
            .build();
    }

    // ── Customer — Profile ────────────────────────────────────────────────

    @Transactional
    public UserInfo updateCustomerProfile(Long userId, String fullName) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> TrimlyException.notFound("User not found"));
        if (fullName != null && !fullName.isBlank())
            user.setFullName(fullName.trim());
        userRepo.save(user);
        return UserInfo.builder()
            .id(user.getId()).fullName(user.getFullName())
            .email(user.getEmail()).phone(user.getPhone()).role(user.getRole())
            .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Booking barberBooking(Long ownerId, Long bookingId) {
        Shop shop = shopRepo.findByOwner_Id(ownerId)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));
        Booking b = bookingRepo.findById(bookingId)
            .orElseThrow(() -> TrimlyException.notFound("Booking not found"));
        if (!b.getShop().getId().equals(shop.getId()))
            throw TrimlyException.forbidden("This booking does not belong to your shop");
        return b;
    }

    BookingResponse toResp(Booking b, boolean showFee) {
        return BookingResponse.builder()
            .id(b.getId())
            .shopId(b.getShop().getId())
            .shopName(b.getShop().getShopName())
            .shopEmoji(b.getShop().getEmoji())
            .customerId(b.getCustomer().getId())
            .customerName(b.getCustomer().getFullName())
            .customerPhone(b.getCustomer().getPhone())
            .employeeId(b.getEmployee() != null ? b.getEmployee().getId() : null)
            .employeeName(b.getEmployee() != null ? b.getEmployee().getName() : null)
            .employeeAvatar(b.getEmployee() != null ? b.getEmployee().getAvatar() : null)
            .employeeRole(b.getEmployee() != null ? b.getEmployee().getRole() : null)
            .employeeSnapshot(b.getEmployeeSnapshot())
            .employeeRating(b.getEmployeeRating())
            .servicesSnapshot(b.getServicesSnapshot())
            .bookingDate(b.getBookingDate()).slotTime(b.getSlotTime())
            .durationMinutes(b.getDurationMinutes()).seats(b.getSeats())
            .totalAmount(b.getTotalAmount())
            .platformFee(showFee ? b.getPlatformFee() : null)
            .barberEarning(showFee ? b.getBarberEarning() : null)
            .status(b.getStatus()).cancelReason(b.getCancelReason())
            .rating(b.getRating()).review(b.getReview())
            .rescheduleDate(b.getRescheduleDate()).rescheduleTime(b.getRescheduleTime())
            .rescheduleReason(b.getRescheduleReason()).rescheduleStatus(b.getRescheduleStatus())
            .createdAt(b.getCreatedAt())
            .build();
    }
}
