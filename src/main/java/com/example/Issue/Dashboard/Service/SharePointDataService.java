package com.example.Issue.Dashboard.Service;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.Issue.Dashboard.Config.SharePointConfig;
import com.example.Issue.Dashboard.Repository.SharePointMessage;
import com.example.Issue.Dashboard.Repository.SharePointMessageRepository;

@Service
public class SharePointDataService {
    
    @Autowired
    private SharePointConfig sharePointConfig;
    
    @Autowired
    private SharePointAuthService authService;
    
    @Autowired
    private SharePointMessageRepository repository;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    
    public Map<String, String> fetchMessagesAsMap() throws Exception {
        try {
            HttpHeaders headers = authService.createHeaders();
            HttpEntity<String> entity = new HttpEntity<>("", headers);
            
            String listUrl = sharePointConfig.getListItemsUrl();
            System.out.println("Fetching from SharePoint: " + listUrl);
            
            ResponseEntity<String> response = restTemplate.exchange(
                listUrl,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new Exception("Failed to fetch data from SharePoint. Status: " + response.getStatusCode());
            }
            
            return parseXmlResponseToMap(response.getBody());
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                throw new Exception("SharePoint list not found. Please verify the list name and URL.");
            } else if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new Exception("Access denied to SharePoint. Please check permissions.");
            }
            throw new Exception("Failed to fetch data from SharePoint: " + e.getMessage());
        } catch (Exception e) {
            throw new Exception("Error fetching SharePoint data: " + e.getMessage());
        }
    }
    
    private Map<String, String> parseXmlResponseToMap(String xmlResponse) throws Exception {
        Map<String, String> messagesMap = new HashMap<>();
        
        try {
            XMLStreamReader reader = xmlFactory.createXMLStreamReader(new StringReader(xmlResponse));
            
            String messageId = null;
            String messageText = null;
            boolean inEntry = false;
            boolean inProperties = false;
            
            while (reader.hasNext()) {
                int event = reader.next();
                
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();
                    
                    if ("entry".equals(localName)) {
                        inEntry = true;
                        messageId = null;
                        messageText = null;
                    } else if (inEntry && "content".equals(localName)) {
                        inProperties = true;
                    } else if (inProperties && "d_MessageId".equals(localName)) {
                        messageId = reader.getElementText();
                    } else if (inProperties && "d_MessageText".equals(localName)) {
                        messageText = reader.getElementText();
                    }
                    // Also handle other common XML namespace patterns
                    else if (inProperties && localName.endsWith("MessageId")) {
                        messageId = reader.getElementText();
                    } else if (inProperties && localName.endsWith("MessageText")) {
                        messageText = reader.getElementText();
                    }
                    
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String localName = reader.getLocalName();
                    
                    if ("entry".equals(localName)) {
                        if (messageId != null && messageText != null) {
                            messagesMap.put(messageId, messageText);
                            System.out.println("Parsed message - ID: " + messageId + ", Text: " + messageText.substring(0, Math.min(50, messageText.length())) + "...");
                        }
                        inEntry = false;
                        messageId = null;
                        messageText = null;
                    } else if ("content".equals(localName)) {
                        inProperties = false;
                    }
                }
            }
            
            reader.close();
            
            System.out.println("Successfully parsed " + messagesMap.size() + " messages from SharePoint");
            return messagesMap;
            
        } catch (Exception e) {
            throw new Exception("Failed to parse SharePoint XML response: " + e.getMessage());
        }
    }
    
    public Map<String, String> fetchAndStoreMessagesAsMap() throws Exception {
        try {
            Map<String, String> messagesMap = fetchMessagesAsMap();
            List<SharePointMessage> storedMessages = new java.util.ArrayList<>();
            
            for (Map.Entry<String, String> entry : messagesMap.entrySet()) {
                String messageId = entry.getKey();
                String messageText = entry.getValue();
                
                // Check if message already exists
                if (repository.findBySharePointItemId(messageId).isEmpty()) {
                    SharePointMessage message = new SharePointMessage();
                    message.setSharePointItemId(messageId);
                    message.setMessageText(messageText);
                    message.setRetrievedAt(LocalDateTime.now());
                    message.setSource("SHAREPOINT");
                    
                    SharePointMessage saved = repository.save(message);
                    storedMessages.add(saved);
                    System.out.println("Stored new message with ID: " + messageId);
                } else {
                    System.out.println("Message with ID " + messageId + " already exists, skipping");
                }
            }
            
            System.out.println("Stored " + storedMessages.size() + " new messages in database");
            return messagesMap;
            
        } catch (Exception e) {
            throw new Exception("Error fetching and storing SharePoint messages: " + e.getMessage());
        }
    }
    
    public Map<String, String> getStoredMessagesAsMap() {
        List<SharePointMessage> allMessages = repository.findAll();
        return allMessages.stream()
                .collect(Collectors.toMap(
                    SharePointMessage::getSharePointItemId,
                    SharePointMessage::getMessageText,
                    (existing, replacement) -> existing
                ));
    }
    
    public List<SharePointMessage> getAllStoredMessages() {
        return repository.findAll();
    }
    
    public long getStoredMessageCount() {
        return repository.count();
    }
    
    public boolean testConnection() {
        return authService.testConnection();
    }
}