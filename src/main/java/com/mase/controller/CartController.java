package com.mase.controller;

import java.net.URI;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import com.mase.dto.CartDto;
import com.mase.service.CartService;

@RestController
@RequestMapping("/api/ecommerce/v1/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public ResponseEntity<CartDto> createCart() {
        CartDto created = cartService.createCart();
        return ResponseEntity.created(URI.create("/api/ecommerce/v1/carts/" + created.cartId()))
                .body(created);
    }

    @GetMapping("/{cartId}")
    public CartDto getCart(@PathVariable String cartId) {
        return cartService.getCart(cartId);
    }

    @PostMapping("/{cartId}/products/{productId}")
    public CartDto addProduct(@PathVariable String cartId, @PathVariable Long productId) {
        return cartService.addProduct(cartId, productId);
    }

    @DeleteMapping("/{cartId}/products/{productId}")
    public CartDto removeProduct(@PathVariable String cartId, @PathVariable Long productId) {
        return cartService.removeProduct(cartId, productId);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CartDto> getAllCarts() {
        return cartService.getAllCarts();
    }
}
