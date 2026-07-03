package com.carwash.backend.repository;

import com.carwash.backend.entity.ShopSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopSettingRepository extends JpaRepository<ShopSetting, String> {
}
