package com.trimly.repository;

import com.trimly.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByShop_IdOrderByDisplayOrderAsc(Long shopId);

    List<Employee> findByShop_IdAndActiveOrderByDisplayOrderAsc(Long shopId, boolean active);

    long countByShop_Id(Long shopId);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.employee.id = :empId
          AND b.bookingDate = :date
          AND b.status NOT IN ('REJECTED','CANCELLED')
        """)
    long countBookingsByEmployeeAndDate(
        @Param("empId") Long empId,
        @Param("date")  LocalDate date);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.employee.id = :empId
          AND b.bookingDate = :date
          AND b.status = 'COMPLETED'
        """)
    long countCompletedByEmployeeAndDate(
        @Param("empId") Long empId,
        @Param("date")  LocalDate date);

    @Query("""
        SELECT COALESCE(SUM(b.barberEarning), 0) FROM Booking b
        WHERE b.employee.id = :empId
          AND b.bookingDate = :date
          AND b.status = 'COMPLETED'
        """)
    BigDecimal earningsByEmployeeAndDate(
        @Param("empId") Long empId,
        @Param("date")  LocalDate date);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.employee.id = :empId
          AND b.bookingDate >= :from AND b.bookingDate <= :to
          AND b.status NOT IN ('REJECTED','CANCELLED')
        """)
    long countBookingsByEmployeeDateRange(
        @Param("empId") Long empId,
        @Param("from")  LocalDate from,
        @Param("to")    LocalDate to);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.employee.id = :empId
          AND b.bookingDate >= :from AND b.bookingDate <= :to
          AND b.status = 'COMPLETED'
        """)
    long countCompletedByEmployeeDateRange(
        @Param("empId") Long empId,
        @Param("from")  LocalDate from,
        @Param("to")    LocalDate to);

    @Query("""
        SELECT COALESCE(SUM(b.barberEarning), 0) FROM Booking b
        WHERE b.employee.id = :empId
          AND b.bookingDate >= :from AND b.bookingDate <= :to
          AND b.status = 'COMPLETED'
        """)
    BigDecimal earningsByEmployeeDateRange(
        @Param("empId") Long empId,
        @Param("from")  LocalDate from,
        @Param("to")    LocalDate to);
}
