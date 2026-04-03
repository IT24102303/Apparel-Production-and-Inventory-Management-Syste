package org.example.pim_system.controller;

import org.example.pim_system.model.InventoryItem;
import org.example.pim_system.repository.InventoryItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @PostMapping("/add-item")
    public ResponseEntity<Map<String, Object>> addInventoryItem(@RequestBody Map<String, String> itemData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            InventoryItem item = new InventoryItem();
            item.setItemName(itemData.get("itemName"));
            item.setCategory(itemData.get("category"));
            item.setCurrentStock(Integer.parseInt(itemData.get("initialQuantity")));
            item.setMinimumStockLevel(Integer.parseInt(itemData.get("minimumStockLevel")));
            item.setUnit(itemData.get("unit"));
            
            // Generate item code
            String itemCode = generateItemCode();
            item.setItemCode(itemCode);
            
            inventoryItemRepository.save(item);
            
            response.put("success", true);
            response.put("message", "Item added to inventory successfully! Item Code: " + itemCode);
            response.put("itemCode", itemCode);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error adding item to inventory: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/items")
    public ResponseEntity<List<InventoryItem>> getAllItems() {
        List<InventoryItem> items = inventoryItemRepository.findAllByOrderByItemNameAsc();
        return ResponseEntity.ok(items);
    }
    
    @PutMapping("/update-stock")
    public ResponseEntity<Map<String, Object>> updateStock(@RequestBody Map<String, String> stockData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String itemCode = stockData.get("itemCode");
            String transactionType = stockData.get("transactionType");
            Integer quantity = Integer.parseInt(stockData.get("quantity"));
            // String reason = stockData.get("reason"); // Reserved for future audit logging
            
            InventoryItem item = inventoryItemRepository.findByItemCode(itemCode)
                    .orElseThrow(() -> new RuntimeException("Item not found with code: " + itemCode));
            
            int oldStock = item.getCurrentStock();
            int newStock;
            
            if ("add".equalsIgnoreCase(transactionType)) {
                newStock = oldStock + quantity;
            } else if ("remove".equalsIgnoreCase(transactionType)) {
                newStock = oldStock - quantity;
                if (newStock < 0) {
                    response.put("success", false);
                    response.put("message", "Cannot remove more stock than available. Current stock: " + oldStock);
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                response.put("success", false);
                response.put("message", "Invalid transaction type. Use 'add' or 'remove'.");
                return ResponseEntity.badRequest().body(response);
            }
            
            item.setCurrentStock(newStock);
            inventoryItemRepository.save(item);
            
            response.put("success", true);
            response.put("message", String.format("Stock updated successfully! %s %d %s. New stock: %d %s", 
                transactionType.equalsIgnoreCase("add") ? "Added" : "Removed", 
                quantity, item.getUnit(), newStock, item.getUnit()));
            response.put("oldStock", oldStock);
            response.put("newStock", newStock);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating stock: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/item/{itemCode}")
    public ResponseEntity<InventoryItem> getItemByCode(@PathVariable String itemCode) {
        return inventoryItemRepository.findByItemCode(itemCode)
                .map(item -> ResponseEntity.ok(item))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/item/{id}")
    public ResponseEntity<Map<String, Object>> deleteItem(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!inventoryItemRepository.existsById(id)) {
                response.put("success", false);
                response.put("message", "Item not found!");
                return ResponseEntity.status(404).body(response);
            }
            
            inventoryItemRepository.deleteById(id);
            
            response.put("success", true);
            response.put("message", "Item deleted successfully!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting item: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    private String generateItemCode() {
        return "ITM-" + String.format("%03d", getNextItemNumber());
    }

    private int getNextItemNumber() {
        // Find the highest existing item code (e.g., ITM-012) and increment it
        return inventoryItemRepository.findTopByOrderByItemCodeDesc()
                .map(item -> {
                    String code = item.getItemCode();
                    if (code == null) {
                        return 1;
                    }
                    int dashIndex = code.lastIndexOf('-');
                    String numberPart = dashIndex >= 0 && dashIndex < code.length() - 1
                            ? code.substring(dashIndex + 1)
                            : code;
                    try {
                        return Integer.parseInt(numberPart) + 1;
                    } catch (NumberFormatException ex) {
                        // Fallback: if existing code is in an unexpected format, fall back to count-based
                        long count = inventoryItemRepository.count();
                        return (int) count + 1;
                    }
                })
                .orElse(1);
    }
}

