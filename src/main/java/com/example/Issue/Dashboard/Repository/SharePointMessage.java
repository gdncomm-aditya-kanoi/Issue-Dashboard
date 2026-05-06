package com.example.Issue.Dashboard.Repository;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Data;

@Document(collection = "sharepoint_messages")
@Data
public class SharePointMessage {

    @Id
    private String id;
    
    @Indexed
    private String messageText;
    
    @Indexed
    private LocalDateTime retrievedAt;
    
    @Indexed
    private String sharePointItemId;
    
    private boolean categorized = false;
    private String category;
    private String source = "SHAREPOINT";
    private String sender;
    private String channelName;
    private LocalDateTime messageTime;
    private String messageId;
}