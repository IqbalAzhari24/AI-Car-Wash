package com.carwash.backend.service;

import com.carwash.backend.entity.User;
import com.carwash.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("Users table is empty. Seeding local environment staffing roster...");

            List<User> initialUsers = new ArrayList<>();

            // 1. Seed Owner
            initialUsers.add(createUser("owner@timahwash.com", "OwnerPass123!", "+60123456789", User.UserRole.OWNER));

            // 2. Seed Clerks (phones: +60123456790, +60123456791)
            for (int i = 1; i <= 2; i++) {
                initialUsers.add(createUser("clerk" + i + "@timahwash.com", "ClerkPass123!", "+6012345679" + (i - 1), User.UserRole.CLERK));
            }

            // 3. Seed Workers (phones: +60123456792 .. +60123456796)
            for (int i = 1; i <= 5; i++) {
                initialUsers.add(createUser("worker" + i + "@timahwash.com", "WorkerPass123!", "+6012345679" + (i + 1), User.UserRole.WORKER));
            }

            // 4. Seed Customer
            initialUsers.add(createUser("customer@gmail.com", "CustomerPass123!", "+60198765432", User.UserRole.CUSTOMER));

            userRepository.saveAll(initialUsers);
            log.info("Successfully seeded 9 default users into the environment with phone profiles.");
        } else {
            log.info("Users table is already populated. Skipping environment seed.");
        }
    }

    private User createUser(String email, String plainPassword, String phoneNumber, User.UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        user.setPhoneNumber(phoneNumber);
        user.setRole(role);
        return user;
    }
}