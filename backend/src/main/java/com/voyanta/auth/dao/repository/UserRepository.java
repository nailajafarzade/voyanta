package com.voyanta.auth.dao.repository;


import com.voyanta.auth.dao.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for the User entity. Besides the standard CRUD methods it
 * looks a user up by email during login and OAuth sign-in, and checks whether an email
 * is already taken during registration.
 */
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}