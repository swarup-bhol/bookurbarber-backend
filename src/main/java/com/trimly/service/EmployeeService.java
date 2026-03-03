package com.trimly.service;

import com.trimly.dto.*;
import com.trimly.entity.*;
import com.trimly.exception.TrimlyException;
import com.trimly.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository  empRepo;
    private final ShopRepository      shopRepo;
    private final SlotBlockRepository blockRepo;
    private final BookingRepository   bookingRepo;

    // ── Public (used in customer booking flow) ────────────────────────────

    public List<EmployeeResponse> getPublicEmployees(Long shopId) {
        return empRepo.findByShop_IdAndActiveOrderByDisplayOrderAsc(shopId, true)
            .stream()
            .map(e -> toLiveResponse(e, shopId))
            .collect(Collectors.toList());
    }

    // ── Owner CRUD ────────────────────────────────────────────────────────

    public List<EmployeeResponse> getMyEmployees(Long ownerId) {
        Shop shop = getShop(ownerId);
        return empRepo.findByShop_IdOrderByDisplayOrderAsc(shop.getId())
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public EmployeeResponse addEmployee(Long ownerId, EmployeeRequest req) {
        Shop shop = getShop(ownerId);
        int order = (int) empRepo.countByShop_Id(shop.getId());
        Employee emp = empRepo.save(Employee.builder()
            .shop(shop)
            .name(req.getName().trim())
            .role(req.getRole())
            .avatar(req.getAvatar() != null ? req.getAvatar() : "💈")
            .phone(req.getPhone())
            .bio(req.getBio())
            .specialties(req.getSpecialties())
            .active(req.getActive() != null ? req.getActive() : true)
            .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : order)
            .build());
        return toResponse(emp);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long ownerId, Long empId, EmployeeRequest req) {
        Employee emp = getOwnedEmployee(ownerId, empId);
        if (StringUtils.hasText(req.getName()))       emp.setName(req.getName().trim());
        if (StringUtils.hasText(req.getRole()))       emp.setRole(req.getRole());
        if (StringUtils.hasText(req.getAvatar()))     emp.setAvatar(req.getAvatar());
        if (StringUtils.hasText(req.getPhone()))      emp.setPhone(req.getPhone());
        if (req.getBio() != null)                     emp.setBio(req.getBio());
        if (req.getSpecialties() != null)             emp.setSpecialties(req.getSpecialties());
        if (req.getActive() != null)                  emp.setActive(req.getActive());
        if (req.getDisplayOrder() != null)            emp.setDisplayOrder(req.getDisplayOrder());
        return toResponse(empRepo.save(emp));
    }

    @Transactional
    public EmployeeResponse toggleActive(Long ownerId, Long empId) {
        Employee emp = getOwnedEmployee(ownerId, empId);
        emp.setActive(!emp.isActive());
        return toResponse(empRepo.save(emp));
    }

    @Transactional
    public void deleteEmployee(Long ownerId, Long empId) {
        empRepo.delete(getOwnedEmployee(ownerId, empId));
    }

    // ── Reports ───────────────────────────────────────────────────────────

    public List<EmployeeStatsResponse> getDailyReport(Long ownerId, LocalDate date) {
        Shop shop = getShop(ownerId);
        LocalDate reportDate  = date != null ? date : LocalDate.now();
        YearMonth month       = YearMonth.from(reportDate);

        return empRepo.findByShop_IdOrderByDisplayOrderAsc(shop.getId())
            .stream()
            .map(e -> buildStats(e, reportDate, month))
            .collect(Collectors.toList());
    }

    // ── Rating updater (called by BookingService after customer rates) ─────

    @Transactional
    public void updateEmployeeRating(Long empId) {
        Employee emp = empRepo.findById(empId)
            .orElseThrow(() -> TrimlyException.notFound("Employee not found"));

        List<Integer> ratings = bookingRepo
            .findByShop_IdOrderByCreatedAtDesc(emp.getShop().getId())
            .stream()
            .filter(b -> b.getEmployee() != null
                      && b.getEmployee().getId().equals(empId)
                      && b.getEmployeeRating() != null)
            .map(b -> b.getEmployeeRating())
            .collect(Collectors.toList());

        if (!ratings.isEmpty()) {
            double avg = ratings.stream().mapToInt(Integer::intValue).average().orElse(0);
            emp.setAvgRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            emp.setTotalReviews(ratings.size());
            empRepo.save(emp);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private EmployeeStatsResponse buildStats(Employee e, LocalDate date, YearMonth month) {
        long todayCount      = empRepo.countBookingsByEmployeeAndDate(e.getId(), date);
        long todayCompleted  = empRepo.countCompletedByEmployeeAndDate(e.getId(), date);
        BigDecimal todayEarn = empRepo.earningsByEmployeeAndDate(e.getId(), date);
        long monthCount      = empRepo.countBookingsByEmployeeDateRange(e.getId(), month.atDay(1), month.atEndOfMonth());
        long monthCompleted  = empRepo.countCompletedByEmployeeDateRange(e.getId(), month.atDay(1), month.atEndOfMonth());
        BigDecimal monthEarn = empRepo.earningsByEmployeeDateRange(e.getId(), month.atDay(1), month.atEndOfMonth());
        long blocked         = blockRepo.countByShop_IdAndBlockDateAndEmployee_Id(e.getShop().getId(), date, e.getId());

        return EmployeeStatsResponse.builder()
            .employeeId(e.getId())
            .employeeName(e.getName())
            .employeeAvatar(e.getAvatar())
            .employeeRole(e.getRole())
            .bookingsToday((int) todayCount)
            .completedToday((int) todayCompleted)
            .earningsToday(todayEarn != null ? todayEarn : BigDecimal.ZERO)
            .bookingsMonth((int) monthCount)
            .completedMonth((int) monthCompleted)
            .earningsMonth(monthEarn != null ? monthEarn : BigDecimal.ZERO)
            .totalBookings(e.getTotalBookings())
            .totalEarnings(e.getTotalEarnings())
            .avgRating(e.getAvgRating())
            .totalReviews(e.getTotalReviews())
            .blockedSlotsToday((int) blocked)
            .build();
    }

    EmployeeResponse toLiveResponse(Employee e, Long shopId) {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();

        boolean busy = bookingRepo
            .findByShop_IdAndBookingDate(shopId, today)
            .stream()
            .anyMatch(b -> b.getEmployee() != null
                && b.getEmployee().getId().equals(e.getId())
                && b.getSlotTime().isBefore(now.plusMinutes(30))
                && b.getSlotTime().isAfter(now.minusMinutes(30))
                && (b.getStatus().name().equals("CONFIRMED")
                    || b.getStatus().name().equals("PENDING")));

        return EmployeeResponse.builder()
            .id(e.getId())
            .shopId(shopId)
            .name(e.getName())
            .role(e.getRole())
            .avatar(e.getAvatar())
            .bio(e.getBio())
            .specialties(e.getSpecialties())
            .active(e.isActive())
            .displayOrder(e.getDisplayOrder())
            .avgRating(e.getAvgRating())
            .totalReviews(e.getTotalReviews())
            .totalBookings(e.getTotalBookings())
            .currentlyBusy(busy)
            .build();
    }

    public EmployeeResponse toResponse(Employee e) {
        return EmployeeResponse.builder()
            .id(e.getId())
            .shopId(e.getShop().getId())
            .name(e.getName())
            .role(e.getRole())
            .avatar(e.getAvatar())
            .phone(e.getPhone())
            .bio(e.getBio())
            .specialties(e.getSpecialties())
            .active(e.isActive())
            .displayOrder(e.getDisplayOrder())
            .avgRating(e.getAvgRating())
            .totalReviews(e.getTotalReviews())
            .totalBookings(e.getTotalBookings())
            .totalEarnings(e.getTotalEarnings())
            .build();
    }

    private Shop getShop(Long ownerId) {
        return shopRepo.findByOwner_Id(ownerId)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));
    }

    private Employee getOwnedEmployee(Long ownerId, Long empId) {
        Shop shop = getShop(ownerId);
        Employee emp = empRepo.findById(empId)
            .orElseThrow(() -> TrimlyException.notFound("Employee not found"));
        if (!emp.getShop().getId().equals(shop.getId()))
            throw TrimlyException.forbidden("Not your employee");
        return emp;
    }
}
