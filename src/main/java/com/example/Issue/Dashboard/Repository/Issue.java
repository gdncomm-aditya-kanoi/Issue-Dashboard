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
  @Indexed
  private String text;
  @Indexed
  private String username;
  private String category;
  @Indexed
  private LocalDateTime timestamp;
  private boolean processed = false;
  private String severity;
  private String source = "TEAMS";

}