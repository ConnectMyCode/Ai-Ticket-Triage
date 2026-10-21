package com.supporttriage.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.supporttriage.entity.User;

@DataJpaTest
@ActiveProfiles("Test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmailShouldReturnUser() {

        // Arrange
        User user = new User();
        user.setEmail("repository@test.com");
        user.setPasswordHash("password123");

        userRepository.save(user);

        // Act
        Optional<User> result =
                userRepository.findByEmail("repository@test.com");

        // Assert
        assertTrue(result.isPresent());

        assertEquals(
                "repository@test.com",
                result.get().getEmail());

        assertEquals(
                "password123",
                result.get().getPasswordHash());
    }
}