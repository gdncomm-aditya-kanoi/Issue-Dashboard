package com.example.Issue.Dashboard.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import com.example.Issue.Dashboard.Config.SharePointConfig;

@Service
public class SharePointAuthService {
    
    @Autowired
    private SharePointConfig sharePointConfig;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");
        return headers;
    }
    
    public boolean testConnection() {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>("", headers);
            
            String listUrl = sharePointConfig.getListItemsUrl();
            System.out.println("Testing connection to: " + listUrl);
            
            ResponseEntity<String> response = restTemplate.exchange(
                listUrl,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (HttpClientErrorException e) {
            System.err.println("Connection test failed - Status: " + e.getStatusCode());
            System.err.println("Response: " + e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            System.err.println("Connection test error: " + e.getMessage());
            return false;
        }
    }
}