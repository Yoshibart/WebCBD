package com.mase.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

// Unit tests for Cart entity behavior.
class CartTest {

    @Test
    // Verifies bidirectional add/remove of products.
    void addAndRemoveProduct_updatesBothSides() {
        Cart cart = new Cart();
        Product product = new Product("Item", "Category", new BigDecimal("10.00"), "Desc");
        product.setId(5L);

        cart.addProduct(product);
        assertTrue(cart.getProducts().contains(product));
        assertTrue(product.getCarts().contains(cart));

        cart.removeProduct(product);
        assertFalse(cart.getProducts().contains(product));
        assertFalse(product.getCarts().contains(cart));
    }

    @Test
    // Verifies publicId is generated when missing.
    void ensurePublicId_generatesWhenMissing() {
        Cart cart = new Cart();
        cart.setPublicId(" ");

        cart.ensurePublicId();

        assertNotNull(cart.getPublicId());
        assertFalse(cart.getPublicId().isBlank());
    }
}
