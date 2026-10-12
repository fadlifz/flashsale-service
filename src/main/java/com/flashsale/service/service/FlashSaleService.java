package com.flashsale.service.service;

import com.flashsale.service.entity.Inventory;
import com.flashsale.service.entity.Order;
import com.flashsale.service.entity.Product;
import com.flashsale.service.repository.InventoryRepository;
import com.flashsale.service.repository.OrderRepository;
import com.flashsale.service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;

    @Autowired 
    private RedissonClient redissonClient;

    @Transactional
    public Order createOrderNaive(Long userId,Long productId, int quantity) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow(() -> new IllegalArgumentException("Inventory not found"));

        if (inventory.getAvailableStock() < quantity) {
            throw new IllegalStateException("Stok habis!");
        }

        inventory.setAvailableStock(inventory.getAvailableStock() - quantity);
        inventoryRepository.save(inventory);

        Order order = Order.builder()
                .userId(userId)
                .product(product)
                .quantity(quantity)
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .status(Order.OrderStatus.PENDING)
                .build();

        return orderRepository.save(order);
    }

    @Transactional
    public Order createOrderPessimistic(Long userId, Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Mengunci baris (row) di DB sampai transaksi ini selesai
        Inventory inventory = inventoryRepository.findByProductIdWithPessimisticLock(productId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));

        if (inventory.getAvailableStock() < quantity) {
            throw new IllegalStateException("Stok habis!");
        }

        inventory.setAvailableStock(inventory.getAvailableStock() - quantity);
        inventoryRepository.save(inventory);

        Order order = Order.builder()
                .userId(userId)
                .product(product)
                .quantity(quantity)
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .status(Order.OrderStatus.PENDING)
                .build();

        return orderRepository.save(order);
    }


    @Transactional 
    public Order createOrderOptimistic(Long userId, Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Menggunakan optimistic locking
        Inventory inventory = inventoryRepository.findByProductIdWithOptimisticLock(productId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));

        if (inventory.getAvailableStock() < quantity) {
            throw new IllegalStateException("Stok habis!");
        }

        inventory.setAvailableStock(inventory.getAvailableStock() - quantity);
        inventoryRepository.save(inventory);

        Order order = Order.builder()
                .userId(userId)
                .product(product)
                .quantity(quantity)
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .status(Order.OrderStatus.PENDING)
                .build();

        return orderRepository.save(order);
    }

    @Transactional
    public Order createOrderAtomic(Long userId, Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Mengurangi stok secara atomik
        int updatedRows = inventoryRepository.decrementStockAtomic(productId, quantity);
        if (updatedRows == 0) {
            throw new IllegalStateException("Stok habis!");
        }

        Order order = Order.builder()
                .userId(userId)
                .product(product)
                .quantity(quantity)
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .status(Order.OrderStatus.PENDING)
                .build();

        return orderRepository.save(order);
    }

    @org.springframework.cache.annotation.Cacheable(value = "products", key = "#productId")
    public Product getProductById(Long productId) {
        System.out.println(">>> Fetching product from MySQL DB for ID: " + productId);
        return productRepository.findById(productId).orElse(null);
    }

    public Order createOrderDistributedLock(Long userId, Long productId, Integer quantity) {
        String lockKey = "lock:product:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Coba acquire lock selama 5 detik, masa simpan lock (lease time) 3 detik
            boolean isAcquired = lock.tryLock(5, 3, java.util.concurrent.TimeUnit.SECONDS);
            if (!isAcquired) {
                throw new IllegalStateException("Sistem sibuk, silakan coba beberapa saat lagi.");
            }

            // Panggil metode eksekusi transaksi terpisah agar @Transactional berjalan benar
            return executeOrderTransaction(userId, productId, quantity);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Terjadi kesalahan pada server", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    public Order executeOrderTransaction(Long userId, Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));

        if (inventory.getAvailableStock() < quantity) {
            throw new IllegalStateException("Stok habis!");
        }

        inventory.setAvailableStock(inventory.getAvailableStock() - quantity);
        inventoryRepository.save(inventory);

        Order order = Order.builder()
                .userId(userId)
                .product(product)
                .quantity(quantity)
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .status(Order.OrderStatus.PENDING)
                .build();

        return orderRepository.save(order);
    }    
}