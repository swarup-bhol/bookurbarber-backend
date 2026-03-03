package com.trimly.repository;

import com.trimly.entity.BarberService;
import com.trimly.enums.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BarberServiceRepository extends JpaRepository<BarberService, Long> {
    List<BarberService> findByShop_IdAndEnabledTrue(Long shopId);
    List<BarberService> findByShop_IdAndCategory(Long shopId, ServiceCategory category);
    List<BarberService> findByShop_Id(Long shopId);
}
