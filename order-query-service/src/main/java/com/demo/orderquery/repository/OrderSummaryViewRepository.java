package com.demo.orderquery.repository;

import com.demo.orderquery.entity.OrderSummaryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderSummaryViewRepository extends JpaRepository<OrderSummaryView, Long> {

    /**
     * O(1) lookup using unique index on user_id.
     * This is the key performance advantage of the read model:
     * single row lookup vs full table COUNT(*) on write DB.
     */
    Optional<OrderSummaryView> findByUserId(Long userId);
}
