package com.mase.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mase.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
