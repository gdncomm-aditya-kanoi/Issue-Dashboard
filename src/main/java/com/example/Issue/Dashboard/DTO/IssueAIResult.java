package com.example.Issue.Dashboard.DTO;

import java.util.List;

import lombok.Data;

@Data
public class IssueAIResult {
  private String issueId;
  private String category;
  private String severity;
  private String summary;
  private List<String> tags;
  private String subCategory;
  private String rootCauseHint;
  private String impact;
  private String suggestedAction;
}
