package com.trimly.config;

import com.trimly.entity.*;
import com.trimly.enums.*;
import com.trimly.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

/**
 * Seeds demo data on first startup (skips if any users already exist).
 *
 * Demo credentials:
 *   Admin:    admin@trimly.app  /  admin123
 *   Barber 1: rajan@blade.com   /  barber123
 *   Barber 2: suresh@dapper.com /  barber123
 *   Barber 3: irfan@royal.com   /  barber123  (pending approval)
 *   Customer: +91 9811111111    (OTP login — OTP sent via WhatsApp)
 */
@Component @RequiredArgsConstructor @Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository        userRepo;
    private final ShopRepository        shopRepo;
    private final BarberServiceRepository svcRepo;
    private final BookingRepository     bookingRepo;
    private final PasswordEncoder       encoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepo.count() > 0) {
            log.info("DB already seeded — skipping");
            return;
        }
        log.info("🌱 Seeding Trimly demo data...");
        seedAdmin();
        Shop shop1 = seedShop1();
        Shop shop2 = seedShop2();
        Shop shop3 = seedShop3(); // pending — no bookings
        seedBookings(shop1, shop2);
        log.info("✅ Seed complete");
        log.info("   Admin:    admin@trimly.app / admin123");
        log.info("   Barber 1: rajan@blade.com / barber123  (shop: Blade & Co., Koramangala Bangalore)");
        log.info("   Barber 2: suresh@dapper.com / barber123 (shop: The Dapper Den, Indiranagar Bangalore)");
        log.info("   Barber 3: irfan@royal.com / barber123  (shop: Royal Cuts, HSR Layout — PENDING)");
        log.info("   Customer: amit@customer.com / customer123  (or use phone OTP: 9811111111)");
    }

    private void seedAdmin() {
        userRepo.save(User.builder()
            .fullName("Platform Admin").email("admin@trimly.app")
            .password(encoder.encode("admin123")).phone("9000000000").role(Role.ADMIN).build());
    }

    private User seedCustomer(String name, String email, String phone) {
        return userRepo.save(User.builder()
            .fullName(name).email(email)
            .password(encoder.encode("customer123")).phone(phone).role(Role.CUSTOMER).build());
    }

    private Shop seedShop1() {
        User rajan = userRepo.save(User.builder()
            .fullName("Rajan Sharma").email("rajan@blade.com")
            .password(encoder.encode("barber123")).phone("9876543210").role(Role.BARBER).build());

        return shopRepo.save(Shop.builder()
            .shopName("Blade & Co.").slug("blade-and-co")
            .location("Koramangala, Bangalore").city("Bangalore").area("Koramangala")
            .latitude(new BigDecimal("12.9352")).longitude(new BigDecimal("77.6245"))
            .bio("Premium barbershop in the heart of Koramangala. Expert fades, beard styling & grooming since 2018.")
            .emoji("✂️").phone("9876543210").color1("#1a1200").color2("#0d0d1a")
            .status(ShopStatus.ACTIVE).plan(PlanType.PRO).isOpen(true).seats(3)
            .commissionPercent(new BigDecimal("10")).subscriptionFee(new BigDecimal("999"))
            .workDays("Mon,Tue,Wed,Thu,Fri,Sat").openTime(LocalTime.of(9, 0)).closeTime(LocalTime.of(20, 0))
            .slotDurationMinutes(30).avgRating(new BigDecimal("4.90")).totalReviews(312).totalBookings(128)
            .monthlyRevenue(new BigDecimal("45000")).owner(rajan).build());
    }

    private Shop seedShop2() {
        User suresh = userRepo.save(User.builder()
            .fullName("Suresh Pillai").email("suresh@dapper.com")
            .password(encoder.encode("barber123")).phone("9887654321").role(Role.BARBER).build());

        return shopRepo.save(Shop.builder()
            .shopName("The Dapper Den").slug("the-dapper-den")
            .location("Indiranagar, Bangalore").city("Bangalore").area("Indiranagar")
            .latitude(new BigDecimal("12.9784")).longitude(new BigDecimal("77.6408"))
            .bio("Modern grooming studio for the contemporary gentleman. Walk-ins welcome.")
            .emoji("💈").phone("9887654321").color1("#0f0a1a").color2("#0d0d1a")
            .status(ShopStatus.ACTIVE).plan(PlanType.STARTER).isOpen(true).seats(2)
            .commissionPercent(new BigDecimal("10")).subscriptionFee(new BigDecimal("499"))
            .workDays("Mon,Tue,Wed,Thu,Fri,Sat,Sun").openTime(LocalTime.of(10, 0)).closeTime(LocalTime.of(21, 0))
            .slotDurationMinutes(30).avgRating(new BigDecimal("4.60")).totalReviews(178).totalBookings(89)
            .monthlyRevenue(new BigDecimal("28000")).owner(suresh).build());
    }

    private Shop seedShop3() {
        User irfan = userRepo.save(User.builder()
            .fullName("Mohammed Irfan").email("irfan@royal.com")
            .password(encoder.encode("barber123")).phone("9865432109").role(Role.BARBER).build());

        Shop shop = shopRepo.save(Shop.builder()
            .shopName("Royal Cuts").slug("royal-cuts")
            .location("HSR Layout, Bangalore").city("Bangalore").area("HSR Layout")
            .latitude(new BigDecimal("12.9082")).longitude(new BigDecimal("77.6476"))
            .bio("Luxury barbering experience. Premium cuts and royal treatment.")
            .emoji("👑").phone("9865432109").color1("#1a0f00").color2("#0a0a14")
            .status(ShopStatus.PENDING).plan(PlanType.PRO).isOpen(false).seats(4)
            .commissionPercent(new BigDecimal("10")).subscriptionFee(new BigDecimal("999"))
            .workDays("Mon,Tue,Wed,Thu,Fri,Sat").openTime(LocalTime.of(9, 0)).closeTime(LocalTime.of(20, 0))
            .slotDurationMinutes(30).owner(irfan).build());

        svcRepo.saveAll(List.of(
            BarberService.builder().shop(shop).serviceName("Royal Haircut").description("Signature luxury cut").category(ServiceCategory.HAIR).price(new BigDecimal("500")).durationMinutes(45).icon("👑").build(),
            BarberService.builder().shop(shop).serviceName("Beard Art").description("Intricate beard designs").category(ServiceCategory.BEARD).price(new BigDecimal("350")).durationMinutes(40).icon("🪒").build(),
            BarberService.builder().shop(shop).serviceName("Skin Ritual").description("Premium facial treatment").category(ServiceCategory.FACIAL).price(new BigDecimal("800")).durationMinutes(60).icon("🌿").build()
        ));
        return shop;
    }

    private void seedBookings(Shop shop1, Shop shop2) {
        // Seed services for shop1
        List<BarberService> s1svcs = svcRepo.saveAll(List.of(
            BarberService.builder().shop(shop1).serviceName("Classic Haircut").description("Precision cut, wash & style").category(ServiceCategory.HAIR).price(new BigDecimal("250")).durationMinutes(30).icon("✂️").build(),
            BarberService.builder().shop(shop1).serviceName("Beard Trim").description("Shape, edge & oil").category(ServiceCategory.BEARD).price(new BigDecimal("150")).durationMinutes(20).icon("🪒").build(),
            BarberService.builder().shop(shop1).serviceName("Clean Shave").description("Hot towel straight razor shave").category(ServiceCategory.BEARD).price(new BigDecimal("200")).durationMinutes(25).icon("🪒").build(),
            BarberService.builder().shop(shop1).serviceName("Skin Fade").description("Taper or skin fade with design").category(ServiceCategory.HAIR).price(new BigDecimal("350")).durationMinutes(45).icon("💈").build(),
            BarberService.builder().shop(shop1).serviceName("Head Massage").description("Relaxing scalp massage with oil").category(ServiceCategory.SPA).price(new BigDecimal("300")).durationMinutes(30).icon("💆").build(),
            BarberService.builder().shop(shop1).serviceName("Royal Package").description("Haircut + Beard + Massage combo").category(ServiceCategory.COMBO).price(new BigDecimal("599")).durationMinutes(75).icon("👑").isCombo(true).build(),
            BarberService.builder().shop(shop1).serviceName("Kids Haircut").description("Fun & gentle cut for kids").category(ServiceCategory.KIDS).price(new BigDecimal("150")).durationMinutes(20).icon("👦").build()
        ));

        // Seed services for shop2
        List<BarberService> s2svcs = svcRepo.saveAll(List.of(
            BarberService.builder().shop(shop2).serviceName("Premium Fade").description("Skin or taper fade").category(ServiceCategory.HAIR).price(new BigDecimal("400")).durationMinutes(45).icon("💈").build(),
            BarberService.builder().shop(shop2).serviceName("Beard Sculpt").description("Designer beard sculpting").category(ServiceCategory.BEARD).price(new BigDecimal("250")).durationMinutes(30).icon("🪒").build(),
            BarberService.builder().shop(shop2).serviceName("Kids Cut").description("Under 10, gentle cut").category(ServiceCategory.KIDS).price(new BigDecimal("180")).durationMinutes(20).icon("👧").build(),
            BarberService.builder().shop(shop2).serviceName("Hair Color").description("Full color treatment").category(ServiceCategory.COLOR).price(new BigDecimal("800")).durationMinutes(90).icon("🎨").enabled(false).build(),
            BarberService.builder().shop(shop2).serviceName("Skin Facial").description("Deep cleanse & moisturise").category(ServiceCategory.FACIAL).price(new BigDecimal("450")).durationMinutes(40).icon("🌿").build(),
            BarberService.builder().shop(shop2).serviceName("Dapper Combo").description("Fade + Beard Sculpt").category(ServiceCategory.COMBO).price(new BigDecimal("599")).durationMinutes(75).icon("👑").isCombo(true).build()
        ));

        // Customers
        User amit  = seedCustomer("Amit Verma",    "amit@customer.com",  "9811111111");
        User priya = seedCustomer("Priya Singh",   "priya@customer.com", "9822222222");
        User rahul = seedCustomer("Rahul Mehta",   "rahul@customer.com", "9833333333");
        User neha  = seedCustomer("Neha Sharma",   "neha@customer.com",  "9844444444");

        BigDecimal comm10 = new BigDecimal("10");

        // Shop 1 bookings
        bookingRepo.saveAll(List.of(
            // Pending — for tomorrow
            makeBooking(shop1, amit,  "Classic Haircut, Beard Trim", LocalDate.now().plusDays(1), LocalTime.of(10, 0), 50, new BigDecimal("400"), comm10),
            makeBooking(shop1, priya, "Royal Package",               LocalDate.now().plusDays(1), LocalTime.of(11, 0), 75, new BigDecimal("599"), comm10),
            makeBooking(shop1, neha,  "Skin Fade",                   LocalDate.now().plusDays(1), LocalTime.of(14, 0), 45, new BigDecimal("350"), comm10),

            // Confirmed — today
            makeBookingStatus(shop1, rahul, "Classic Haircut", LocalDate.now(), LocalTime.of(9, 0), 30, new BigDecimal("250"), comm10, BookingStatus.CONFIRMED),

            // Completed — with ratings
            makeBookingRated(shop1, amit,  "Skin Fade",    LocalDate.now().minusDays(3), LocalTime.of(14, 0), 45, new BigDecimal("350"), comm10, 5, "Best fade in Bangalore!"),
            makeBookingRated(shop1, priya, "Classic Haircut", LocalDate.now().minusDays(7), LocalTime.of(9, 0), 30, new BigDecimal("250"), comm10, 5, null),
            makeBookingRated(shop1, rahul, "Beard Trim",   LocalDate.now().minusDays(14), LocalTime.of(11, 0), 20, new BigDecimal("150"), comm10, 4, "Quick and clean"),

            // Cancelled
            makeBookingStatus(shop1, neha, "Head Massage", LocalDate.now().minusDays(2), LocalTime.of(16, 0), 30, new BigDecimal("300"), comm10, BookingStatus.CANCELLED)
        ));

        // Shop 2 bookings
        bookingRepo.saveAll(List.of(
            makeBooking(shop2, rahul, "Dapper Combo",  LocalDate.now().plusDays(1), LocalTime.of(12, 0), 75, new BigDecimal("599"), comm10),
            makeBookingRated(shop2, amit,  "Premium Fade", LocalDate.now().minusDays(5), LocalTime.of(15, 0), 45, new BigDecimal("400"), comm10, 4, null),
            makeBookingRated(shop2, priya, "Skin Facial",  LocalDate.now().minusDays(10), LocalTime.of(11, 0), 40, new BigDecimal("450"), comm10, 5, "Loved it!")
        ));
    }

    private Booking makeBooking(Shop shop, User customer, String services,
            LocalDate date, LocalTime time, int duration, BigDecimal amount, BigDecimal commPct) {
        return makeBookingStatus(shop, customer, services, date, time, duration, amount, commPct, BookingStatus.PENDING);
    }

    private Booking makeBookingStatus(Shop shop, User customer, String services,
            LocalDate date, LocalTime time, int duration, BigDecimal amount, BigDecimal commPct, BookingStatus status) {
        BigDecimal fee = amount.multiply(commPct).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        return Booking.builder()
            .shop(shop).customer(customer).servicesSnapshot(services).serviceIds("")
            .bookingDate(date).slotTime(time).durationMinutes(duration).seats(1)
            .totalAmount(amount).platformFee(fee).barberEarning(amount.subtract(fee))
            .status(status).build();
    }

    private Booking makeBookingRated(Shop shop, User customer, String services,
            LocalDate date, LocalTime time, int duration, BigDecimal amount, BigDecimal commPct,
            int rating, String review) {
        Booking b = makeBookingStatus(shop, customer, services, date, time, duration, amount, commPct, BookingStatus.COMPLETED);
        b.setRating(rating);
        b.setReview(review);
        return b;
    }
}
