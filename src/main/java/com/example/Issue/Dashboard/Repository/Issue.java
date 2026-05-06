package com.example.Issue.Dashboard.Repository;

import java.time.LocalDateTime;
import java.util.List;

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

  // AI Enriched Fields
  private String category;
  private String subCategory;     // finer classification
  private String severity;        // LOW / MEDIUM / HIGH / CRITICAL
  private String sentiment;       // NEGATIVE / NEUTRAL / POSITIVE
  private String summary;         // short 1-line summary
  private List<String> tags;      // keywords like ["SAP", "timeout"]

  // Derived Insights
  private String rootCauseHint;   // AI guess
  private String impact;          // business impact
  private String suggestedAction; // next step

  // Analytics Helpers
  private String module;          // e.g. INVENTORY_UI, PR_SERVICE
  private String duplicateOf;     // messageId if duplicate
  private boolean isDuplicate;

  private boolean processed = false;

}