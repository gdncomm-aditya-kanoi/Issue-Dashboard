package com.example.Issue.Dashboard.DTO;

import lombok.Data;

@Data
public class TeamsMessage {
    private String text;        // actual message content
    private String user;        // sender name
    private String channel;     // optional (Teams channel)
    private String timestamp;
}