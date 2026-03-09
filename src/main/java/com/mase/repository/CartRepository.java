package com.mase.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mase.model.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByPublicId(String publicId);
}
