package com.demo.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * User entity - stored in user_db (separate database per service in SOA/Microservices)
 * This demonstrates the "Database per Service" pattern which leads to the
 * Distributed Query Problem: we CANNOT do: SELECT u.*, o.* FROM users u JOIN orders o ON u.id = o.user_id
 * because they live in different databases (possibly different servers)
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;
    private String fullName;
    private String phone;
}
