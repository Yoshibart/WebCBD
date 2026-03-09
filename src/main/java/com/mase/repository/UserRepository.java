package com.mase.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mase.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
