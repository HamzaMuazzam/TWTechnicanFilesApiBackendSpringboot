package com.trackingworld.tw.alerts.repo;

import com.trackingworld.tw.alerts.domain.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AlertTypeRepository extends JpaRepository<AlertType, String>, JpaSpecificationExecutor<AlertType> {

    List<AlertType> findByActiveTrueOrderBySortOrderAscCodeAsc();

    List<AlertType> findAllByOrderBySortOrderAscCodeAsc();

    @Query("select coalesce(max(t.sortOrder), 0) from AlertType t")
    int maxSortOrder();
}
