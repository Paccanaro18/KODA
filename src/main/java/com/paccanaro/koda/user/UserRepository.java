package com.paccanaro.koda.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** O e-mail chega aqui ja normalizado para minusculas. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
