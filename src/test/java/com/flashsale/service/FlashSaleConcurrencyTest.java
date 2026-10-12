package com.flashsale.service;

import com.flashsale.service.entity.Inventory;
import com.flashsale.service.entity.Product;
import com.flashsale.service.repository.InventoryRepository;
import com.flashsale.service.repository.OrderRepository;
import com.flashsale.service.repository.ProductRepository;
import com.flashsale.service.service.FlashSaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class FlashSaleConcurrencyTest {

    @Autowired
    private FlashSaleService flashSaleService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Long productId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();

        Product product = productRepository.save(Product.builder()
                .name("Flash Sale Laptop")
                .description("Laptop Gaming High Spec")
                .price(BigDecimal.valueOf(10000000))
                .build());

        productId = product.getId();

        inventoryRepository.save(Inventory.builder()
                .product(product)
                .totalStock(10)
                .availableStock(10) // Stok hanya 10
                .version(0L)
                .build());
    }

    @Test
    @DisplayName("Uji 50 Request Bersamaan dengan Pessimistic Lock - Stok Harus Tepat 0 dan Order Dibuat 10")
    void testPessimisticLockConcurrency() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            long userId = i + 1;
            executorService.submit(() -> {
                try {
                    flashSaleService.createOrderPessimistic(userId, productId, 1);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow();
        long totalOrders = orderRepository.count();

        System.out.println("====== HASIL PESSIMISTIC LOCK ======");
        System.out.println("Berhasil Order: " + successCount.get());
        System.out.println("Gagal Order   : " + failCount.get());
        System.out.println("Sisa Stok DB  : " + inventory.getAvailableStock());
        System.out.println("Total Order DB: " + totalOrders);

        assertEquals(10, successCount.get());
        assertEquals(0, inventory.getAvailableStock());
        assertEquals(10, totalOrders);
    }
}