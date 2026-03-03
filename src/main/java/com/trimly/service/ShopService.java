package com.trimly.service;

import com.trimly.dto.*;
import com.trimly.entity.*;
import com.trimly.enums.BookingStatus;
import com.trimly.enums.ShopStatus;
import com.trimly.exception.TrimlyException;
import com.trimly.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
@Transactional
public class ShopService {

    private final ShopRepository          shopRepo;
    private final BarberServiceRepository svcRepo;
    private final BookingRepository       bookingRepo;
    private final SlotBlockRepository     blockRepo;
    private final EmployeeRepository      empRepo;

    // ── Public browsing ───────────────────────────────────────────────────

    public List<ShopResponse> getPublicShops(String q, String city, String area) {
        String qn    = StringUtils.hasText(q)    ? q    : null;
        String cityN = StringUtils.hasText(city) ? city : null;
        String areaN = StringUtils.hasText(area) ? area : null;
        return shopRepo.searchActive(qn, cityN, areaN).stream()
            .map(this::toPublic).collect(Collectors.toList());
    }

    public ShopResponse getPublicShopById(Long id) {
        Shop s = shopRepo.findById(id)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));
        if (s.getStatus() != ShopStatus.ACTIVE) throw TrimlyException.notFound("Shop not available");
        return toPublic(s);
    }

    public ShopResponse getPublicShopBySlug(String slug) {
        Shop s = shopRepo.findBySlug(slug)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));
        if (s.getStatus() != ShopStatus.ACTIVE) throw TrimlyException.notFound("Shop not available");
        return toPublic(s);
    }

    /**
     * Slot availability — now respects manual blocks and shows per-employee status.
     *
     * @param shopId     shop id
     * @param date       the date to check
     * @param employeeId optional — if set, only that employee's availability is considered
     */
    public SlotAvailabilityResponse getSlots(Long shopId, LocalDate date, Long employeeId) {
        Shop shop = shopRepo.findById(shopId)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));
        if (shop.getStatus() != ShopStatus.ACTIVE)
            throw TrimlyException.notFound("Shop not available");

        List<Employee> employees = empRepo.findByShop_IdAndActiveOrderByDisplayOrderAsc(shopId, true);
        List<SlotInfo> slots     = genSlotsWithSeats(shop, date, employeeId, employees);

        return SlotAvailabilityResponse.builder()
            .date(date)
            .slots(slots)
            .totalSlots(slots.size())
            .availableSlots((int) slots.stream().filter(SlotInfo::isAvailable).count())
            .build();
    }

    /** Backwards-compatible overload — no employee filter */
    public SlotAvailabilityResponse getSlots(Long shopId, LocalDate date) {
        return getSlots(shopId, date, null);
    }

    public LocationMeta getLocationMeta() {
        List<String> cities = shopRepo.findActiveCities();
        Map<String, List<String>> areasByCity = new LinkedHashMap<>();
        for (String city : cities) areasByCity.put(city, shopRepo.findActiveAreasInCity(city));
        return LocationMeta.builder().cities(cities).areasByCity(areasByCity).build();
    }

    // ── Barber ────────────────────────────────────────────────────────────

    public ShopResponse getBarberShop(Long ownerId) {
        return toBarber(shopRepo.findByOwner_Id(ownerId)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found")));
    }

    @Transactional
    public ShopResponse updateShop(Long ownerId, ShopUpdateRequest req) {
        Shop s = shopRepo.findByOwner_Id(ownerId)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));

        if (StringUtils.hasText(req.getShopName()))       s.setShopName(req.getShopName().trim());
        if (StringUtils.hasText(req.getLocation()))       s.setLocation(req.getLocation().trim());
        if (StringUtils.hasText(req.getCity()))           s.setCity(req.getCity().trim());
        if (StringUtils.hasText(req.getArea()))           s.setArea(req.getArea().trim());
        if (req.getLatitude() != null)                    s.setLatitude(req.getLatitude());
        if (req.getLongitude() != null)                   s.setLongitude(req.getLongitude());
        if (StringUtils.hasText(req.getBio()))            s.setBio(req.getBio().trim());
        if (StringUtils.hasText(req.getEmoji()))          s.setEmoji(req.getEmoji().trim());
        if (StringUtils.hasText(req.getPhone()))          s.setPhone(req.getPhone().trim());
        if (req.getIsOpen() != null)                      s.setOpen(req.getIsOpen());
        if (req.getSeats() != null)                       s.setSeats(req.getSeats());
        if (StringUtils.hasText(req.getWorkDays()))       s.setWorkDays(req.getWorkDays());
        if (req.getOpenTime() != null)                    s.setOpenTime(req.getOpenTime());
        if (req.getCloseTime() != null)                   s.setCloseTime(req.getCloseTime());
        if (req.getSlotDurationMinutes() != null)         s.setSlotDurationMinutes(req.getSlotDurationMinutes());

        return toBarber(shopRepo.save(s));
    }

    @Transactional
    public ServiceResponse addService(Long ownerId, ServiceRequest req) {
        Shop shop = shopRepo.findByOwner_Id(ownerId)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));
        BarberService svc = svcRepo.save(BarberService.builder()
            .shop(shop)
            .serviceName(req.getServiceName().trim())
            .description(req.getDescription())
            .category(req.getCategory())
            .price(req.getPrice())
            .durationMinutes(req.getDurationMinutes())
            .icon(req.getIcon() != null ? req.getIcon() : "✂️")
            .isCombo(req.isCombo())
            .build());
        return toSvcResp(svc, shop.getCommissionPercent(), true);
    }

    @Transactional
    public ServiceResponse updateService(Long ownerId, Long svcId, ServiceRequest req) {
        Shop shop = shopRepo.findByOwner_Id(ownerId)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));
        BarberService svc = svcRepo.findById(svcId)
            .orElseThrow(() -> TrimlyException.notFound("Service not found"));
        if (!svc.getShop().getId().equals(shop.getId()))
            throw TrimlyException.forbidden("Not your service");

        if (StringUtils.hasText(req.getServiceName())) svc.setServiceName(req.getServiceName().trim());
        if (req.getDescription() != null)              svc.setDescription(req.getDescription());
        if (req.getCategory() != null)                 svc.setCategory(req.getCategory());
        if (req.getPrice() != null)                    svc.setPrice(req.getPrice());
        if (req.getDurationMinutes() > 0)              svc.setDurationMinutes(req.getDurationMinutes());
        if (StringUtils.hasText(req.getIcon()))        svc.setIcon(req.getIcon());

        return toSvcResp(svcRepo.save(svc), shop.getCommissionPercent(), true);
    }

    @Transactional
    public ServiceResponse toggleService(Long ownerId, Long svcId) {
        Shop shop = shopRepo.findByOwner_Id(ownerId)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));
        BarberService svc = svcRepo.findById(svcId)
            .orElseThrow(() -> TrimlyException.notFound("Service not found"));
        if (!svc.getShop().getId().equals(shop.getId()))
            throw TrimlyException.forbidden("Not your service");
        svc.setEnabled(!svc.isEnabled());
        return toSvcResp(svcRepo.save(svc), shop.getCommissionPercent(), true);
    }

    @Transactional
    public void deleteService(Long ownerId, Long svcId) {
        Shop shop = shopRepo.findByOwner_Id(ownerId)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));
        BarberService svc = svcRepo.findById(svcId)
            .orElseThrow(() -> TrimlyException.notFound("Service not found"));
        if (!svc.getShop().getId().equals(shop.getId()))
            throw TrimlyException.forbidden("Not your service");
        svcRepo.delete(svc);
    }

    // ── Admin ─────────────────────────────────────────────────────────────

    public List<ShopResponse> getAllAdmin() {
        return shopRepo.findAllByOrderByCreatedAtDesc().stream()
            .map(this::toBarber).collect(Collectors.toList());
    }

    @Transactional
    public ShopResponse setStatus(Long shopId, ShopStatus status) {
        Shop s = shopRepo.findById(shopId)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));
        s.setStatus(status);
        if (status == ShopStatus.DISABLED) s.setOpen(false);
        return toBarber(shopRepo.save(s));
    }

    @Transactional
    public ShopResponse updateCommission(Long shopId, BigDecimal pct) {
        if (pct.compareTo(BigDecimal.ZERO) < 0 || pct.compareTo(new BigDecimal("50")) > 0)
            throw TrimlyException.badRequest("Commission must be 0–50%");
        Shop s = shopRepo.findById(shopId)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));
        s.setCommissionPercent(pct);
        return toBarber(shopRepo.save(s));
    }

    // ── Slot generation ───────────────────────────────────────────────────

    private List<SlotInfo> genSlotsWithSeats(Shop shop, LocalDate date,
                                              Long filterEmployeeId,
                                              List<Employee> employees) {
        List<SlotInfo> list = new ArrayList<>();
        LocalTime t = shop.getOpenTime();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("h:mm a");
        int seats = shop.getSeats();

        // Load all blocks for this shop+date in one query
        List<SlotBlock> allBlocks = blockRepo.findByShop_IdAndBlockDate(shop.getId(), date);
        Set<LocalTime> shopWideBlocked = allBlocks.stream()
            .filter(b -> b.getEmployee() == null)
            .map(SlotBlock::getSlotTime)
            .collect(Collectors.toSet());
        Map<Long, Set<LocalTime>> empBlockedMap = new HashMap<>();
        for (SlotBlock b : allBlocks) {
            if (b.getEmployee() != null) {
                empBlockedMap
                    .computeIfAbsent(b.getEmployee().getId(), k -> new HashSet<>())
                    .add(b.getSlotTime());
            }
        }

        // Load all bookings for this shop+date in one query
        List<Booking> dayBookings = bookingRepo.findByShop_IdAndBookingDate(shop.getId(), date);

        while (t.isBefore(shop.getCloseTime())) {
            final LocalTime slotTime = t;

            boolean blockedShopWide = shopWideBlocked.contains(slotTime);
            int used   = (int) dayBookings.stream()
                .filter(b -> b.getSlotTime().equals(slotTime)
                    && b.getStatus() != BookingStatus.REJECTED
                    && b.getStatus() != BookingStatus.CANCELLED)
                .mapToInt(Booking::getSeats).sum();
            int left   = Math.max(0, seats - used);
            boolean takenBySeat = left == 0;
            boolean blockedByOwner = blockedShopWide;
            boolean available = !takenBySeat && !blockedByOwner;

            // If filtering by employee, also check employee-specific block and bookings
            if (filterEmployeeId != null) {
                boolean empBlocked = shopWideBlocked.contains(slotTime) ||
                    empBlockedMap.getOrDefault(filterEmployeeId, Collections.emptySet()).contains(slotTime);
                boolean empBooked = dayBookings.stream().anyMatch(b ->
                    b.getEmployee() != null
                    && b.getEmployee().getId().equals(filterEmployeeId)
                    && b.getSlotTime().equals(slotTime)
                    && b.getStatus() != BookingStatus.REJECTED
                    && b.getStatus() != BookingStatus.CANCELLED);
                available = !takenBySeat && !empBlocked && !empBooked;
                blockedByOwner = empBlocked;
            }

            // Build per-employee statuses for this slot
            List<EmployeeSlotStatus> empStatuses = new ArrayList<>();
            for (Employee emp : employees) {
                boolean empBlocked = shopWideBlocked.contains(slotTime) ||
                    empBlockedMap.getOrDefault(emp.getId(), Collections.emptySet()).contains(slotTime);
                boolean empBooked = dayBookings.stream().anyMatch(b ->
                    b.getEmployee() != null
                    && b.getEmployee().getId().equals(emp.getId())
                    && b.getSlotTime().equals(slotTime)
                    && b.getStatus() != BookingStatus.REJECTED
                    && b.getStatus() != BookingStatus.CANCELLED);
                empStatuses.add(EmployeeSlotStatus.builder()
                    .employeeId(emp.getId())
                    .employeeName(emp.getName())
                    .employeeAvatar(emp.getAvatar())
                    .available(!empBlocked && !empBooked && !takenBySeat)
                    .blocked(empBlocked)
                    .booked(empBooked)
                    .build());
            }

            list.add(SlotInfo.builder()
                .time(slotTime)
                .label(slotTime.format(fmt))
                .taken(takenBySeat)
                .available(available)
                .blockedByOwner(blockedByOwner)
                .seatsTotal(seats)
                .seatsUsed(used)
                .seatsLeft(left)
                .employeeStatuses(empStatuses)
                .build());

            t = t.plusMinutes(shop.getSlotDurationMinutes());
        }
        return list;
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    ServiceResponse toSvcResp(BarberService s, BigDecimal commPct, boolean showFee) {
        BigDecimal fee = null, earn = null;
        if (showFee && s.getPrice() != null) {
            fee  = s.getPrice().multiply(commPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            earn = s.getPrice().subtract(fee);
        }
        return ServiceResponse.builder()
            .id(s.getId()).serviceName(s.getServiceName()).description(s.getDescription())
            .category(s.getCategory()).price(s.getPrice()).durationMinutes(s.getDurationMinutes())
            .icon(s.getIcon()).enabled(s.isEnabled()).isCombo(s.isCombo())
            .platformFee(fee).barberEarning(earn).build();
    }

    public ShopResponse toPublic(Shop s) {
        List<ServiceResponse> svcs = s.getServices().stream()
            .filter(BarberService::isEnabled)
            .map(sv -> toSvcResp(sv, s.getCommissionPercent(), false))
            .collect(Collectors.toList());
        ShopResponse resp = buildResp(s, svcs, false);

        // Employee quick-view for browse screen
        List<Employee> activeEmps = empRepo.findByShop_IdAndActiveOrderByDisplayOrderAsc(s.getId(), true);
        resp.setTotalEmployees(activeEmps.size());
        if (!activeEmps.isEmpty()) {
            long freeNow = activeEmps.stream()
                .filter(e -> !isCurrentlyBusy(e, s.getId()))
                .count();
            resp.setFreeEmployeesNow((int) freeNow);
        }
        return resp;
    }

    public ShopResponse toBarber(Shop s) {
        List<ServiceResponse> svcs = s.getServices().stream()
            .map(sv -> toSvcResp(sv, s.getCommissionPercent(), true))
            .collect(Collectors.toList());
        return buildResp(s, svcs, true);
    }

    private boolean isCurrentlyBusy(Employee e, Long shopId) {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();
        return bookingRepo.findByShop_IdAndBookingDate(shopId, today).stream()
            .anyMatch(b -> b.getEmployee() != null
                && b.getEmployee().getId().equals(e.getId())
                && b.getSlotTime().isBefore(now.plusMinutes(30))
                && b.getSlotTime().isAfter(now.minusMinutes(30))
                && (b.getStatus() == com.trimly.enums.BookingStatus.CONFIRMED
                    || b.getStatus() == com.trimly.enums.BookingStatus.PENDING));
    }

    private ShopResponse buildResp(Shop s, List<ServiceResponse> svcs, boolean showFee) {
        return ShopResponse.builder()
            .id(s.getId()).shopName(s.getShopName()).slug(s.getSlug())
            .location(s.getLocation()).city(s.getCity()).area(s.getArea())
            .latitude(s.getLatitude()).longitude(s.getLongitude())
            .bio(s.getBio()).emoji(s.getEmoji()).phone(s.getPhone())
            .status(s.getStatus()).plan(s.getPlan()).isOpen(s.isOpen()).seats(s.getSeats())
            .avgRating(s.getAvgRating()).totalReviews(s.getTotalReviews())
            .totalBookings(s.getTotalBookings()).monthlyRevenue(s.getMonthlyRevenue())
            .workDays(s.getWorkDays()).openTime(s.getOpenTime()).closeTime(s.getCloseTime())
            .slotDurationMinutes(s.getSlotDurationMinutes())
            .subscriptionFee(showFee ? s.getSubscriptionFee() : null)
            .commissionPercent(showFee ? s.getCommissionPercent() : null)
            .ownerId(s.getOwner() != null ? s.getOwner().getId() : null)
            .ownerName(s.getOwner() != null ? s.getOwner().getFullName() : null)
            .ownerEmail(s.getOwner() != null ? s.getOwner().getEmail() : null)
            .createdAt(s.getCreatedAt())
            .services(svcs)
            .build();
    }
}
