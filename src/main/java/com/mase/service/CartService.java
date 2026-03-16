package com.mase.service;

import java.util.List;

import com.mase.dto.CartDto;

public interface CartService {

    CartDto createCart();

    CartDto getCart(String cartId);

    CartDto addProduct(String cartId, Long productId);

    CartDto removeProduct(String cartId, Long productId);

    List<CartDto> getAllCarts();
}
