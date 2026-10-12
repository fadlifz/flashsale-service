package com.flashsale.service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.service.dto.OrderEvent;
import com.flashsale.service.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderConsumer {

    private final ObjectMapper objectMapper;
    private final FlashSaleService flashSaleService;

    @KafkaListener(topics = "flashsale-orders", groupId = "flashsale-group")
    public void consumeOrderEvent(String message) {
        try {
            System.out.println(">>> [Kafka Consumer] Menerima pesan dari Topic: " + message);
            OrderEvent event = objectMapper.readValue(message, OrderEvent.class);

            // Eksekusi pemotongan stok & pembuatan order secara asinkron
            flashSaleService.executeOrderTransaction(event.getUserId(), event.getProductId(), event.getQuantity());
            System.out.println(">>> [Kafka Consumer] Order berhasil diproses di background untuk User ID: " + event.getUserId());

        } catch (Exception e) {
            System.err.println(">>> [Kafka Consumer] Gagal memproses order: " + e.getMessage());
        }
    }
}