package com.flashsale.service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.service.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendOrderEvent(OrderEvent event) {
        try {
            String jsonEvent = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("flashsale-orders", event.getProductId().toString(), jsonEvent);
            System.out.println(">>> [Kafka Producer] Message dikirim ke Topic: " + jsonEvent);
        } catch (Exception e) {
            throw new RuntimeException("Gagal mengirim event ke Kafka", e);
        }
    }
}