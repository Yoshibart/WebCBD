package com.mase.service.impl;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mase.dto.CartDto;
import com.mase.model.Cart;
import com.mase.model.Product;
import com.mase.repository.CartRepository;
import com.mase.repository.ProductRepository;
import com.mase.service.CartService;

@Service
public class CartServiceImpl implements CartService {

    private static final String CART_NOT_FOUND_MESSAGE = "Cart not found";
    private static final String PRODUCT_NOT_FOUND_MESSAGE = "Product not found";

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartServiceImpl(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public CartDto createCart() {
        return toDto(cartRepository.save(new Cart()));
    }

    @Override
    @Transactional(readOnly = true)
    public CartDto getCart(String cartId) {
        return toDto(findCart(cartId));
    }

    @Override
    @Transactional
    public CartDto addProduct(String cartId, Long productId) {
        Cart cart = findCart(cartId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PRODUCT_NOT_FOUND_MESSAGE));
        cart.addProduct(product);
        return toDto(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartDto removeProduct(String cartId, Long productId) {
        Cart cart = findCart(cartId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PRODUCT_NOT_FOUND_MESSAGE));
        cart.removeProduct(product);
        return toDto(cartRepository.save(cart));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartDto> getAllCarts() {
        return cartRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    private Cart findCart(String cartId) {
        return cartRepository.findByPublicId(cartId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, CART_NOT_FOUND_MESSAGE));
    }

    private CartDto toDto(Cart cart) {
        return new CartDto(
                cart.getPublicId(),
                cart.getProducts().stream().map(Product::getId).collect(java.util.stream.Collectors.toSet()));
    }
}
