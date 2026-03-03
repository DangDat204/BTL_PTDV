package com.demo.ordercommand.repository;

import com.demo.ordercommand.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    /**
     * === Benchmark: Write DB Count Query ===
     * This demonstrates the "naive" approach - running COUNT(*) + SUM() directly
     * on the normalized orders table in the write DB.
     *
     * Problem: As data grows (millions of orders), this query becomes slow.
     * Full table scan + aggregation on large dataset = high latency.
     * Compare this with the pre-computed read model in order-query-service.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.userId = :userId")
    BigDecimal sumAmountByUserId(@Param("userId") Long userId);
}
