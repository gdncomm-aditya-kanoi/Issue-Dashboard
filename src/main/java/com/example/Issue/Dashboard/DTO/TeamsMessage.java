package com.example.Issue.Dashboard.DTO;

import lombok.Data;

@Data
public class TeamsMessage {
  private String text;
  private String messageTitle;
  private String messageId;
  private String user;
  private String channel;
  private String timestamp;
}