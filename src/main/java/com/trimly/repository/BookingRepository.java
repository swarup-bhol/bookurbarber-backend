package com.trimly.repository;

import com.trimly.entity.Booking;
import com.trimly.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByShop_IdOrderByCreatedAtDesc(Long shopId);
    List<Booking> findByShop_IdAndStatusOrderByCreatedAtDesc(Long shopId, BookingStatus status);
    List<Booking> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);
    List<Booking> findByShop_IdAndBookingDate(Long shopId, LocalDate date);
    List<Booking> findAllByOrderByCreatedAtDesc();
    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

    /**
     * Seat-aware slot availability check.
     * Returns the total seats already booked at a given slot.
     * A slot is available if seatsUsed < shop.seats.
     */
    @Query("""
        SELECT COALESCE(SUM(b.seats), 0)
        FROM Booking b
        WHERE b.shop.id  = :shopId
          AND b.bookingDate = :date
          AND b.slotTime   = :time
          AND b.status NOT IN ('REJECTED', 'CANCELLED')
        """)
    int countSeatsUsedAtSlot(
        @Param("shopId") Long shopId,
        @Param("date")   LocalDate date,
        @Param("time")   LocalTime time
    );

    /** Revenue analytics */
    @Query("SELECT COALESCE(SUM(b.totalAmount),0) FROM Booking b WHERE b.shop.id=:sid AND b.status='COMPLETED'")
    BigDecimal totalRevenueByShop(@Param("sid") Long sid);

    @Query("SELECT COALESCE(SUM(b.platformFee),0) FROM Booking b WHERE b.status='COMPLETED'")
    BigDecimal totalPlatformCommission();

    /** Count helpers */
    long countByShop_Id(Long shopId);
    long countByShop_IdAndStatus(Long shopId, BookingStatus status);
    long countByStatus(BookingStatus status);

    /** Bookings with a pending reschedule for a given customer */
    List<Booking> findByCustomer_IdAndStatus(Long customerId, BookingStatus status);
}
