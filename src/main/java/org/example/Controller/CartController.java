package org.example.Controller;

import org.example.Client.InventoryServiceClient;
import org.example.Entity.CartItem;
import org.example.Event.CheckoutEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartController {
    private final Map<String, List<CartItem>> cartRepository = new HashMap<>();
    private final InventoryServiceClient inventoryServiceClient;
    private final KafkaTemplate<String, CheckoutEvent> kafkaTemplate;


    public CartController(InventoryServiceClient inventoryServiceClient, KafkaTemplate<String, CheckoutEvent> kafkaTemplate) {
        this.inventoryServiceClient = inventoryServiceClient;
        this.kafkaTemplate = kafkaTemplate;
    }
    @PostMapping("/{userId}")
    public ResponseEntity<String> addToCart(@PathVariable String userId, @RequestBody CartItem item) {
        cartRepository.computeIfAbsent(userId, k -> new ArrayList<>()).add(item);
        return ResponseEntity.ok("Item added to cart successfully");
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<CartItem>> viewCart(@PathVariable String userId) {
        return ResponseEntity.ok(cartRepository.getOrDefault(userId, new ArrayList<>()));
    }

    @PostMapping("/{userId}/checkout")
    public ResponseEntity<String> checkout(@PathVariable String userId) {
        List<CartItem> items = cartRepository.remove(userId);
        if (items == null || items.isEmpty()) {
            return ResponseEntity.badRequest().body("Cart is empty");
        }
        // Check stock availability
        for (CartItem item : items) {
            ResponseEntity<Integer> stockResponse = inventoryServiceClient.getStock(item.getProductId());
            int availableStock = stockResponse.getBody();
            if (availableStock == 0 || availableStock < item.getQuantity()) {
                return ResponseEntity.badRequest().body("Insufficient stock for product ID: " + item.getProductId());
            }
        }
        // Deduct stock
        for (CartItem item : items) {
            inventoryServiceClient.updateStock(item.getProductId(), -item.getQuantity());
        }

        // Clear cart after successful checkout
        cartRepository.remove(userId);
        CheckoutEvent event = new CheckoutEvent(userId, items.get(0).getProductId());
        kafkaTemplate.send("checkout-topic", event);

        // Publish checkout event
        System.out.println("Checkout event published for user: " + userId);
        return ResponseEntity.ok("Checkout successful");
    }
}
