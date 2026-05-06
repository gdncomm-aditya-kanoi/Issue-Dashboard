package com.example.Issue.Dashboard.Repository;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Data;

@Document(collection = "issues")
@Data
public class Issue {

  @Id
  private String id;
  
  //Pre AI
  @Indexed(unique = true)
  private String messageId;
  private String messageTitle;
  private String text;
  private String username;
  private String source;
  private LocalDateTime timestamp;

  //Post AI
  private String category;
  private String severity;

  private boolean processed = false;

}