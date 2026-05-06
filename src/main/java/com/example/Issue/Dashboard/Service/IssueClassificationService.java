package com.example.Issue.Dashboard.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.Issue.Dashboard.Config.GeminiClient;
import com.example.Issue.Dashboard.Repository.Issue;
import com.example.Issue.Dashboard.Repository.IssueRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IssueClassificationService {

    private static final List<String> ALLOWED_CATEGORIES = List.of(
            "INVENTORY",
            "PR_MANAGEMENT",
            "DATA_INTEGRITY",
            "SYSTEM_PERFORMANCE",
            "ACCESS_CONTROL",
            "INTEGRATION",
            "WORKFLOW",
            "REPORTING",
            "OTHER");

  private final IssueRepository issueRepository;
  private final GeminiClient geminiClient;
  private final ObjectMapper objectMapper;

  public IssueClassificationService(IssueRepository issueRepository, GeminiClient geminiClient,
      ObjectMapper objectMapper) {
    this.issueRepository = issueRepository;
    this.geminiClient = geminiClient;
    this.objectMapper = objectMapper;
  }


  public Map<String, String> classifyPendingIssues() {
    List<Issue> pendingIssues = issueRepository.findByProcessedFalse();
    Map<String, String> pendingIssueMap = buildIssueMap(pendingIssues);

    if (pendingIssues.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, String> issueCategories = pendingIssueMap.isEmpty() ?
        fallbackCategories(buildIssueIdMap(pendingIssues)) :
        classifyWithGemini(pendingIssueMap);
    List<Issue> updatedIssues = new ArrayList<>();

    for (Issue issue : pendingIssues) {
      String issueId = issue.getMessageId();
      if (issueId == null) {
        continue;
      }

      String category = normalizeCategory(issueCategories.get(issueId));
      issue.setCategory(category);
      issue.setProcessed(true);
      updatedIssues.add(issue);
      issueCategories.put(issueId, category);
    }

    issueRepository.saveAll(updatedIssues);
    return issueCategories;
  }

  private Map<String, String> buildIssueIdMap(List<Issue> issues) {
    Map<String, String> issueIds = new LinkedHashMap<>();
    for (Issue issue : issues) {
      if (issue != null && issue.getMessageId() != null) {
        issueIds.put(issue.getMessageId(), "");
      }
    }
    return issueIds;
  }

  private Map<String, String> buildIssueMap(List<Issue> issues) {
    Map<String, String> issueMap = new LinkedHashMap<>();
    for (Issue issue : issues) {
      if (issue == null || issue.getMessageId() == null) {
        continue;
      }

      String text = issue.getText();
      if (text == null || text.trim().isEmpty()) {
        continue;
      }

      issueMap.put(issue.getMessageId(), text);
    }
    return issueMap;
  }

  private Map<String, String> classifyWithGemini(Map<String, String> pendingIssueMap) {
    try {
      String prompt = buildPrompt(pendingIssueMap);
      String response = geminiClient.generateContent(prompt);
      return parseGeminiResponse(response, pendingIssueMap);
    } catch (Exception e) {
      System.err.println("Gemini classification failed: " + e.getMessage());
      return fallbackCategories(pendingIssueMap);
    }
  }

  private String buildPrompt(Map<String, String> pendingIssueMap) throws IOException {
    String inputJson = objectMapper.writeValueAsString(pendingIssueMap);

        return "You are classifying Microsoft Teams issue messages.\n"
                + "Use only these categories:\n"
                + "- INVENTORY: Stock issues, item problems, quantity mismatches, reserved stock\n"
                + "- PR_MANAGEMENT: Purchase requisition issues, PR creation, PR status, PR data problems\n"
                + "- DATA_INTEGRITY: Duplicate data, data mismatches, synchronization issues\n"
                + "- SYSTEM_PERFORMANCE: Loading issues, timeouts, processing delays, UI freezing\n"
                + "- ACCESS_CONTROL: Permission issues, role problems, authentication\n"
                + "- INTEGRATION: SAP integration, external system connectivity, data exchange\n"
                + "- WORKFLOW: Process issues, approval workflows, state management\n"
                + "- REPORTING: Report generation, data export, analytics issues\n"
                + "- OTHER: Issues that don't fit above categories\n"
                + "Return only a valid JSON array with one object per issue, using exactly this shape:\n"
                + "[{\"issueId\":\"123\",\"category\":\"INVENTORY\"}]\n"
                + "Do not add markdown, explanations, or extra fields.\n"
                + "Classify every issue in the input map.\n"
                + "Input map:\n"
                + inputJson;
    }

  private Map<String, String> parseGeminiResponse(String response, Map<String, String> pendingIssueMap) {
    Map<String, String> issueCategories = new LinkedHashMap<>();

    if (response == null || response.trim().isEmpty()) {
      return fallbackCategories(pendingIssueMap);
    }

    try {
      JsonNode root = objectMapper.readTree(extractJsonPayload(response));

      if (root.isArray()) {
        for (JsonNode item : root) {
          String issueId = textValue(item, "issueId");
          String category = normalizeCategory(textValue(item, "category"));
          if (issueId != null && !issueId.trim().isEmpty() && pendingIssueMap.containsKey(issueId)) {
            issueCategories.put(issueId, category);
          }
        }
      } else if (root.isObject()) {
        root.fields().forEachRemaining(entry -> {
          if (pendingIssueMap.containsKey(entry.getKey())) {
            issueCategories.put(entry.getKey(), normalizeCategory(entry.getValue().asText()));
          }
        });
      }
    } catch (Exception e) {
      return fallbackCategories(pendingIssueMap);
    }

    for (String issueId : pendingIssueMap.keySet()) {
      if (!issueCategories.containsKey(issueId)) {
        issueCategories.put(issueId, "OTHER");
      }
    }

    return issueCategories;
  }

  private String extractJsonPayload(String response) {
    String cleaned = response.trim();

    if (cleaned.startsWith("```")) {
      cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
      cleaned = cleaned.replaceFirst("\\s*```$", "");
    }

    int arrayStart = cleaned.indexOf('[');
    int arrayEnd = cleaned.lastIndexOf(']');
    if (arrayStart >= 0 && arrayEnd > arrayStart) {
      return cleaned.substring(arrayStart, arrayEnd + 1);
    }

    int objectStart = cleaned.indexOf('{');
    int objectEnd = cleaned.lastIndexOf('}');
    if (objectStart >= 0 && objectEnd > objectStart) {
      return cleaned.substring(objectStart, objectEnd + 1);
    }

    return cleaned;
  }

  private Map<String, String> fallbackCategories(Map<String, String> pendingIssueMap) {
    Map<String, String> fallback = new LinkedHashMap<>();
    for (String issueId : pendingIssueMap.keySet()) {
      fallback.put(issueId, "OTHER");
    }
    return fallback;
  }

  private String textValue(JsonNode node, String fieldName) {
    JsonNode field = node.get(fieldName);
    return field == null || field.isNull() ? null : field.asText();
  }

    private String normalizeCategory(String category) {
        if (category == null) {
            return "OTHER";
        }

        String normalized = category.trim().toUpperCase();
        for (String allowedCategory : ALLOWED_CATEGORIES) {
            if (allowedCategory.equals(normalized)) {
                return normalized;
            }
        }
        return "OTHER";
    }
}
