package com.trackingworld.tw.alerts.repo;

import com.trackingworld.tw.alerts.domain.AppUser;
import com.trackingworld.tw.alerts.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {

    Optional<AppUser> findByPhoneE164(String phoneE164);

    boolean existsByIdAndStatus(Long id, UserStatus status);
}
