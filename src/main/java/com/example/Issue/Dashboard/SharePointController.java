package com.example.Issue.Dashboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Issue.Dashboard.Repository.SharePointMessage;
import com.example.Issue.Dashboard.Service.SharePointDataService;

@RestController
@RequestMapping("/sharepoint")
public class SharePointController {

    @Autowired
    private SharePointDataService sharePointDataService;

    @GetMapping("/messages")
    public ResponseEntity<?> getMessagesFromSharePoint() {
        try {
            Map<String, String> messagesMap = sharePointDataService.fetchMessagesAsMap();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", messagesMap.size());
            response.put("messages", messagesMap);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            if (e.getMessage().contains("not found") || e.getMessage().contains("list")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else if (e.getMessage().contains("Access denied") || e.getMessage().contains("permissions")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncMessagesFromSharePoint() {
        try {
            Map<String, String> messagesMap = sharePointDataService.fetchAndStoreMessagesAsMap();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messagesFetched", messagesMap.size());
            response.put("totalStoredMessages", sharePointDataService.getStoredMessageCount());
            response.put("messages", messagesMap);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            if (e.getMessage().contains("not found") || e.getMessage().contains("list")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else if (e.getMessage().contains("Access denied") || e.getMessage().contains("permissions")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/stored/messages")
    public ResponseEntity<?> getStoredMessagesAsMap() {
        try {
            Map<String, String> messagesMap = sharePointDataService.getStoredMessagesAsMap();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", messagesMap.size());
            response.put("messages", messagesMap);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve stored messages: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/stored/details")
    public ResponseEntity<?> getStoredMessagesWithDetails() {
        try {
            List<SharePointMessage> messages = sharePointDataService.getAllStoredMessages();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", messages.size());
            response.put("messages", messages);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve stored message details: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/test-connection")
    public ResponseEntity<?> testSharePointConnection() {
        try {
            boolean connected = sharePointDataService.testConnection();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", connected);
            response.put("connected", connected);
            response.put("message", connected ? "SharePoint connection successful" : "SharePoint connection failed");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("connected", false);
            errorResponse.put("error", "Connection test failed: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getSharePointStatus() {
        try {
            long totalStoredMessages = sharePointDataService.getStoredMessageCount();
            boolean connectionStatus = sharePointDataService.testConnection();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalStoredMessages", totalStoredMessages);
            response.put("connectionStatus", connectionStatus);
            response.put("lastChecked", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get SharePoint status: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}