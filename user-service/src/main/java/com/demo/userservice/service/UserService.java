package com.demo.userservice.service;

import com.demo.userservice.entity.User;
import com.demo.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> findById(Long id) {
        log.info("Fetching user with id: {}", id);
        return userRepository.findById(id);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Seed some demo data
     */
    public void seedData() {
        if (userRepository.count() == 0) {
            userRepository.save(User.builder().username("alice").email("alice@example.com").fullName("Alice Nguyen").phone("0901234567").build());
            userRepository.save(User.builder().username("bob").email("bob@example.com").fullName("Bob Tran").phone("0912345678").build());
            userRepository.save(User.builder().username("charlie").email("charlie@example.com").fullName("Charlie Le").phone("0923456789").build());
            log.info("Seeded 3 demo users");
        }
    }
}
