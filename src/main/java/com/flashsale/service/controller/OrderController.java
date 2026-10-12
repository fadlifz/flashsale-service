package com.flashsale.service.controller;

import com.flashsale.service.entity.Order;
import com.flashsale.service.entity.Product;
import com.flashsale.service.service.FlashSaleService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.flashsale.service.kafka.OrderProducer;

import com.flashsale.service.dto.OrderEvent;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final FlashSaleService flashSaleService;
    
    @Autowired
    private OrderProducer orderProducer;

    @PostMapping("/naive")
    public ResponseEntity<?> placeOrderNaive(@RequestParam Long userId, @RequestParam Long productId, @RequestParam(defaultValue = "1") Integer quantity) {
        try {
            Order order = flashSaleService.createOrderNaive(userId, productId, quantity);
            return ResponseEntity.ok("Order berhasil dibuat dengan ID: " + order.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/pessimistic")
    public ResponseEntity<?> placeOrderPessimistic(@RequestParam Long userId, @RequestParam Long productId, @RequestParam(defaultValue = "1") Integer quantity) {
        try {
            Order order = flashSaleService.createOrderPessimistic(userId, productId, quantity);
            return ResponseEntity.ok("Order [Pessimistic] berhasil: " + order.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/optimistic")
    public ResponseEntity<?> placeOrderOptimistic(@RequestParam Long userId, @RequestParam Long productId, @RequestParam(defaultValue = "1") Integer quantity) {
        try {
            Order order = flashSaleService.createOrderOptimistic(userId, productId, quantity);
            return ResponseEntity.ok("Order [Optimistic] berhasil: " + order.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/atomic")
    public ResponseEntity<?> placeOrderAtomic(@RequestParam Long userId, @RequestParam Long productId, @RequestParam(defaultValue = "1") Integer quantity) {
        try {
            Order order = flashSaleService.createOrderAtomic(userId, productId, quantity);
            return ResponseEntity.ok("Order [Atomic] berhasil: " + order.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        Product product = flashSaleService.getProductById(id);
        if (product == null) {
            throw new IllegalArgumentException("Product not found");
        }
        return ResponseEntity.ok(product);
    }

    @PostMapping("/distributed-lock")
    public ResponseEntity<?> placeOrderDistributedLock(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        try {
            Order order = flashSaleService.createOrderDistributedLock(userId, productId, quantity);
            return ResponseEntity.ok("Order [Distributed Lock] berhasil: " + order.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/async-kafka")
    public ResponseEntity<?> placeOrderKafka(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity) {

        OrderEvent event = OrderEvent.builder()
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .build();

        orderProducer.sendOrderEvent(event);

        return ResponseEntity.ok("Pesanan Anda telah diterima dan sedang diproses di antrean!");
    }

}
