package com.trimly.repository;

import com.trimly.entity.SlotBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface SlotBlockRepository extends JpaRepository<SlotBlock, Long> {

    /** All blocks for a shop on a given date (both shop-wide and employee-specific) */
    List<SlotBlock> findByShop_IdAndBlockDate(Long shopId, LocalDate date);

    /** Blocks for a specific employee on a date */
    List<SlotBlock> findByShop_IdAndBlockDateAndEmployee_Id(Long shopId, LocalDate date, Long empId);

    /** Is a slot blocked shop-wide (employee IS NULL) */
    @Query("""
        SELECT COUNT(sb) > 0 FROM SlotBlock sb
        WHERE sb.shop.id   = :shopId
          AND sb.blockDate = :date
          AND sb.slotTime  = :time
          AND sb.employee IS NULL
        """)
    boolean isSlotBlockedShopWide(
        @Param("shopId") Long shopId,
        @Param("date")   LocalDate date,
        @Param("time")   LocalTime time);

    /** Is a slot blocked for a specific employee OR shop-wide */
    @Query("""
        SELECT COUNT(sb) > 0 FROM SlotBlock sb
        WHERE sb.shop.id   = :shopId
          AND sb.blockDate = :date
          AND sb.slotTime  = :time
          AND (sb.employee IS NULL OR sb.employee.id = :empId)
        """)
    boolean isSlotBlockedForEmployee(
        @Param("shopId") Long shopId,
        @Param("date")   LocalDate date,
        @Param("time")   LocalTime time,
        @Param("empId")  Long empId);

    /** Delete a shop-wide block */
    @Modifying
    @Query("""
        DELETE FROM SlotBlock sb
        WHERE sb.shop.id = :shopId AND sb.blockDate = :date
          AND sb.slotTime = :time AND sb.employee IS NULL
        """)
    void deleteShopWideBlock(
        @Param("shopId") Long shopId,
        @Param("date")   LocalDate date,
        @Param("time")   LocalTime time);

    /** Delete an employee-specific block */
    @Modifying
    @Query("""
        DELETE FROM SlotBlock sb
        WHERE sb.shop.id = :shopId AND sb.blockDate = :date
          AND sb.slotTime = :time AND sb.employee.id = :empId
        """)
    void deleteEmployeeBlock(
        @Param("shopId") Long shopId,
        @Param("date")   LocalDate date,
        @Param("time")   LocalTime time,
        @Param("empId")  Long empId);

    /** Unblock all slots for an employee on a date */
    @Modifying
    @Query("""
        DELETE FROM SlotBlock sb
        WHERE sb.shop.id = :shopId AND sb.blockDate = :date
          AND sb.employee.id = :empId
        """)
    void deleteAllByShopAndDateAndEmployee(
        @Param("shopId") Long shopId,
        @Param("date")   LocalDate date,
        @Param("empId")  Long empId);

    /** Count employee-specific blocks on a date (for reports) */
    long countByShop_IdAndBlockDateAndEmployee_Id(Long shopId, LocalDate date, Long empId);
}
