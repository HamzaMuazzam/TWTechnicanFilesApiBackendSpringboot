package com.trackingworld.tw.alerts.repo;

import com.trackingworld.tw.alerts.domain.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
