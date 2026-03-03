package com.trimly.service;

import com.trimly.dto.*;
import com.trimly.entity.*;
import com.trimly.exception.TrimlyException;
import com.trimly.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotBlockService {

    private final SlotBlockRepository blockRepo;
    private final ShopRepository      shopRepo;
    private final EmployeeRepository  empRepo;

    // ── Queries ───────────────────────────────────────────────────────────

    public List<SlotBlockResponse> getBlockedSlots(Long ownerId, LocalDate date) {
        Shop shop = getShop(ownerId);
        return blockRepo.findByShop_IdAndBlockDate(shop.getId(), date)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<SlotBlockResponse> getBlockedSlotsForEmployee(Long ownerId, LocalDate date, Long empId) {
        Shop shop = getShop(ownerId);
        return blockRepo.findByShop_IdAndBlockDateAndEmployee_Id(shop.getId(), date, empId)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Block single slot ─────────────────────────────────────────────────

    @Transactional
    public SlotBlockResponse blockSlot(Long ownerId, SlotBlockRequest req) {
        Shop shop     = getShop(ownerId);
        Employee emp  = resolveEmployee(req.getEmployeeId(), shop.getId());

        if (emp == null) {
            if (blockRepo.isSlotBlockedShopWide(shop.getId(), req.getBlockDate(), req.getSlotTime()))
                throw TrimlyException.conflict("This slot is already blocked");
        } else {
            if (blockRepo.isSlotBlockedForEmployee(shop.getId(), req.getBlockDate(), req.getSlotTime(), emp.getId()))
                throw TrimlyException.conflict("This slot is already blocked for this employee");
        }

        SlotBlock block = blockRepo.save(SlotBlock.builder()
            .shop(shop)
            .employee(emp)
            .blockDate(req.getBlockDate())
            .slotTime(req.getSlotTime())
            .reason(req.getReason())
            .blockedBy("OWNER")
            .build());

        return toResponse(block);
    }

    // ── Block a range of slots ─────────────────────────────────────────────

    @Transactional
    public List<SlotBlockResponse> blockRange(Long ownerId, SlotBlockRangeRequest req) {
        Shop shop    = getShop(ownerId);
        Employee emp = resolveEmployee(req.getEmployeeId(), shop.getId());

        int slotDuration = shop.getSlotDurationMinutes();
        LocalTime current = req.getFromTime();
        List<SlotBlockResponse> created = new ArrayList<>();

        while (!current.isAfter(req.getToTime().minusMinutes(slotDuration))) {
            final LocalTime slotTime = current;

            boolean alreadyBlocked = emp == null
                ? blockRepo.isSlotBlockedShopWide(shop.getId(), req.getBlockDate(), slotTime)
                : blockRepo.isSlotBlockedForEmployee(shop.getId(), req.getBlockDate(), slotTime, emp.getId());

            if (!alreadyBlocked) {
                SlotBlock block = blockRepo.save(SlotBlock.builder()
                    .shop(shop)
                    .employee(emp)
                    .blockDate(req.getBlockDate())
                    .slotTime(slotTime)
                    .reason(req.getReason())
                    .blockedBy("OWNER")
                    .build());
                created.add(toResponse(block));
            }
            current = current.plusMinutes(slotDuration);
        }
        return created;
    }

    // ── Unblock ───────────────────────────────────────────────────────────

    @Transactional
    public void unblockSlot(Long ownerId, LocalDate date, LocalTime time, Long employeeId) {
        Shop shop = getShop(ownerId);
        if (employeeId == null) {
            blockRepo.deleteShopWideBlock(shop.getId(), date, time);
        } else {
            blockRepo.deleteEmployeeBlock(shop.getId(), date, time, employeeId);
        }
    }

    @Transactional
    public void unblockAllForEmployee(Long ownerId, LocalDate date, Long empId) {
        Shop shop = getShop(ownerId);
        blockRepo.deleteAllByShopAndDateAndEmployee(shop.getId(), date, empId);
    }

    // ── Used internally by ShopService slot generation ────────────────────

    public boolean isBlockedShopWide(Long shopId, LocalDate date, LocalTime time) {
        return blockRepo.isSlotBlockedShopWide(shopId, date, time);
    }

    public boolean isBlockedForEmployee(Long shopId, LocalDate date, LocalTime time, Long empId) {
        return blockRepo.isSlotBlockedForEmployee(shopId, date, time, empId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Employee resolveEmployee(Long empId, Long shopId) {
        if (empId == null) return null;
        Employee emp = empRepo.findById(empId)
            .orElseThrow(() -> TrimlyException.notFound("Employee not found"));
        if (!emp.getShop().getId().equals(shopId))
            throw TrimlyException.forbidden("Employee does not belong to your shop");
        return emp;
    }

    private Shop getShop(Long ownerId) {
        return shopRepo.findByOwner_Id(ownerId)
            .orElseThrow(() -> TrimlyException.notFound("Shop not found"));
    }

    SlotBlockResponse toResponse(SlotBlock sb) {
        return SlotBlockResponse.builder()
            .id(sb.getId())
            .blockDate(sb.getBlockDate())
            .slotTime(sb.getSlotTime())
            .reason(sb.getReason())
            .blockedBy(sb.getBlockedBy())
            .employeeId(sb.getEmployee() != null ? sb.getEmployee().getId() : null)
            .employeeName(sb.getEmployee() != null ? sb.getEmployee().getName() : null)
            .employeeAvatar(sb.getEmployee() != null ? sb.getEmployee().getAvatar() : null)
            .build();
    }
}
