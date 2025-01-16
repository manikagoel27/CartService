package org.example.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "INVENTORY-SERVICE")
public interface InventoryServiceClient {

    @GetMapping("/inventory/{productId}")
    ResponseEntity<Integer> getStock(@PathVariable("productId") String productId);

    @PostMapping("/inventory/{productId}")
    ResponseEntity<String> updateStock(@PathVariable("productId") String productId, @RequestParam("quantity") int quantity);
}