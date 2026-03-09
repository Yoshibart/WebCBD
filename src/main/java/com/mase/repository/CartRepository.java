package com.mase.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mase.model.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
}
