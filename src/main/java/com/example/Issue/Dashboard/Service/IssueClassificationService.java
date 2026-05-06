package com.example.Issue.Dashboard.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.example.Issue.Dashboard.Config.GeminiClient;
import com.example.Issue.Dashboard.DTO.IssueAIResult;
import com.example.Issue.Dashboard.Repository.Issue;
import com.example.Issue.Dashboard.Repository.IssueRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class IssueClassificationService {

  private static final List<String> ALLOWED_CATEGORIES =
      List.of("INVENTORY", "PR_MANAGEMENT", "DATA_INTEGRITY", "SYSTEM_PERFORMANCE", "ACCESS_CONTROL", "INTEGRATION",
          "WORKFLOW", "REPORTING", "OTHER");

  private final IssueRepository issueRepository;
  private final GeminiClient geminiClient;
  private final ObjectMapper objectMapper;
  private final MongoTemplate mongoTemplate;

  public IssueClassificationService(IssueRepository issueRepository, GeminiClient geminiClient,
      ObjectMapper objectMapper, MongoTemplate mongoTemplate) {
    this.issueRepository = issueRepository;
    this.geminiClient = geminiClient;
    this.objectMapper = objectMapper;
    this.mongoTemplate = mongoTemplate;
  }


  public void classifyPendingIssues(List<Issue> unprocessedIssues) throws IOException {
    if (unprocessedIssues.isEmpty()) {
      return;
    }
    Map<String, String> issueMap = new LinkedHashMap<>();
    for (Issue issue : unprocessedIssues) {
      if (issue.getMessageId() != null) {
        issueMap.put(issue.getMessageId(), issue.getText());
      }
    }
    Map<String, IssueAIResult> results = classifyWithGemini(issueMap);

    int updatedCount = 0;
    for (Issue issue : unprocessedIssues) {
      String messageId = issue.getMessageId();
      if (messageId == null) {
        continue;
      }

      IssueAIResult ai = results.get(messageId);
      Update update = new Update().set("category", ai.getCategory()).set("severity", ai.getSeverity())
          .set("summary", ai.getSummary()).set("tags", ai.getTags()).set("subCategory", ai.getSubCategory())
          .set("rootCauseHint", ai.getRootCauseHint()).set("impact", ai.getImpact())
          .set("suggestedAction", ai.getSuggestedAction()).set("processed", true);
      Query query = new Query(Criteria.where("messageId").is(messageId));
      var result = mongoTemplate.updateFirst(query, update, Issue.class);
      if (result.getModifiedCount() > 0) {
        updatedCount++;
      }
    }

    log.info("Successfully updated " + updatedCount + " issues with classifications");
  }


  private Map<String, IssueAIResult> classifyWithGemini(Map<String, String> pendingIssueMap) {
    try {
      String prompt = buildPrompt(pendingIssueMap);
      String response = geminiClient.generateContent(prompt);
      return parseGeminiResponse(response, pendingIssueMap);
    } catch (Exception e) {
      System.err.println("Gemini classification failed: " + e.getMessage());
      return fallbackResults(pendingIssueMap);
    }
  }

  private String buildPrompt(Map<String, String> pendingIssueMap) throws IOException {
    String inputJson = objectMapper.writeValueAsString(pendingIssueMap);
    return "You are an AI system analyzing Microsoft Teams issue messages related to an enterprise Inventory + PR "
        + "(Purchase Requisition) system.\n" + "\n"
        + "Your job is to classify and extract structured insights from each issue.\n" + "\n"
        + "----------------------------------------\n" + "\uD83D\uDD39 INPUT CHARACTERISTICS\n"
        + "----------------------------------------\n"
        + "- Messages may be informal, incomplete, or contain screenshots (ignore image URLs)\n"
        + "- Messages may be questions, bugs, inconsistencies, or requests\n"
        + "- Some messages may NOT be real issues → still classify them\n" + "\n"
        + "----------------------------------------\n" + "\uD83D\uDD39 CLASSIFICATION RULES (VERY IMPORTANT)\n"
        + "----------------------------------------\n" + "\n" + "You MUST assign exactly ONE category from:\n" + "\n"
        + "1. INVENTORY\n" + "- Stock mismatch, stock = 0 issues\n" + "- Reserved stock problems\n"
        + "- SOH (Stock On Hand) inconsistencies\n" + "- Warehouse stock differences\n" + "\n" + "2. PR_MANAGEMENT\n"
        + "- PR creation, PR data, PR fields missing\n" + "- PR state issues (draft, submitted, etc.)\n"
        + "- PR listing problems\n" + "- PR item-related queries\n" + "\n" + "3. DATA_INTEGRITY\n"
        + "- Same data showing different values for different users\n" + "- Mismatch between systems/files\n"
        + "- Duplicate or inconsistent data\n" + "\n" + "4. SYSTEM_PERFORMANCE\n"
        + "- Loading issues, stuck states, UI freezing\n" + "- Buttons disabled due to processing\n"
        + "- Timeouts or slow system\n" + "\n" + "5. ACCESS_CONTROL\n" + "- Permission issues\n"
        + "- User cannot access something\n" + "\n" + "6. INTEGRATION\n" + "- SAP or external system issues\n"
        + "- Data not syncing between systems\n" + "\n" + "7. WORKFLOW\n" + "- Approval issues\n"
        + "- Process flow broken\n" + "\n" + "8. REPORTING\n" + "- Reports, exports, analytics issues\n" + "\n"
        + "9. OTHER\n" + "- General questions, data requests, unclear issues\n" + "\n"
        + "----------------------------------------\n" + "\uD83D\uDD39 FIELD EXTRACTION\n"
        + "----------------------------------------\n" + "\n" + "For each issue extract:\n" + "\n"
        + "1. category → from above (MANDATORY)\n" + "\n" + "2. severity:\n" + "- LOW → question / minor issue\n"
        + "- MEDIUM → incorrect behavior but workaround exists\n" + "- HIGH → blocking user task\n"
        + "- CRITICAL → system unusable\n" + "\n" + "3. summary:\n" + "- One short sentence describing the issue\n"
        + "\n" + "4. tags:\n" + "- 2–5 keywords (e.g., \"PR\", \"stock\", \"SAP\", \"timeout\")\n" + "\n"
        + "5. subCategory:\n" + "- More specific classification (free text)\n" + "\n" + "6. rootCauseHint:\n"
        + "- Likely cause if inferable, else \"UNKNOWN\"\n" + "\n" + "7. impact:\n"
        + "- What is affected (user/system/business)\n" + "\n" + "8. suggestedAction:\n"
        + "- What should be done next\n" + "\n" + "----------------------------------------\n"
        + "\uD83D\uDD39 DECISION GUIDELINES\n" + "----------------------------------------\n" + "\n"
        + "- If message mentions stock mismatch → INVENTORY\n"
        + "- If same data differs between users → DATA_INTEGRITY\n" + "- If PR field missing → PR_MANAGEMENT\n"
        + "- If UI stuck/loading/disabled → SYSTEM_PERFORMANCE\n" + "- If asking for data (not an issue) → OTHER\n"
        + "- If unclear → choose best fit, DO NOT default blindly to OTHER\n" + "\n"
        + "----------------------------------------\n" + "\uD83D\uDD39 EXAMPLES (VERY IMPORTANT)\n"
        + "----------------------------------------\n" + "\n" + "Example 1:\n" + "Input:\n"
        + "\"inventory is 0 but expected 4800\"\n" + "\n" + "Output:\n" + "{\n" + "  \"category\": \"INVENTORY\",\n"
        + "  \"severity\": \"HIGH\",\n" + "  \"summary\": \"Inventory showing zero instead of expected stock\",\n"
        + "  \"tags\": [\"inventory\", \"stock mismatch\"],\n" + "  \"subCategory\": \"Stock discrepancy\",\n"
        + "  \"rootCauseHint\": \"Inventory sync issue\",\n" + "  \"impact\": \"Incorrect stock visibility\",\n"
        + "  \"suggestedAction\": \"Verify warehouse stock sync\"\n" + "}\n" + "\n" + "Example 2:\n" + "Input:\n"
        + "\"SOH is 14 for one user but 4 for another\"\n" + "\n" + "Output:\n" + "{\n"
        + "  \"category\": \"DATA_INTEGRITY\",\n" + "  \"severity\": \"HIGH\",\n"
        + "  \"summary\": \"Different SOH values for same item across users\",\n"
        + "  \"tags\": [\"SOH\", \"data mismatch\"],\n" + "  \"subCategory\": \"Inconsistent data\",\n"
        + "  \"rootCauseHint\": \"Cache or sync issue\",\n" + "  \"impact\": \"Users see different stock values\",\n"
        + "  \"suggestedAction\": \"Check data consistency across systems\"\n" + "}\n" + "\n" + "Example 3:\n"
        + "Input:\n" + "\"send for review button is disabled and shows processing\"\n" + "\n" + "Output:\n" + "{\n"
        + "  \"category\": \"SYSTEM_PERFORMANCE\",\n" + "  \"severity\": \"HIGH\",\n"
        + "  \"summary\": \"PR stuck in processing state with disabled action button\",\n"
        + "  \"tags\": [\"loading\", \"UI\", \"PR\"],\n" + "  \"subCategory\": \"UI stuck\",\n"
        + "  \"rootCauseHint\": \"Backend processing delay\",\n" + "  \"impact\": \"User cannot proceed with PR\",\n"
        + "  \"suggestedAction\": \"Check backend job status\"\n" + "}\n" + "\n" + "Example 4:\n" + "Input:\n"
        + "\"no Lead BR assigned for this PR\"\n" + "\n" + "Output:\n" + "{\n" + "  \"category\": \"PR_MANAGEMENT\",\n"
        + "  \"severity\": \"MEDIUM\",\n" + "  \"summary\": \"Lead BR not assigned in PR\",\n"
        + "  \"tags\": [\"PR\", \"missing field\"],\n" + "  \"subCategory\": \"Missing assignment\",\n"
        + "  \"rootCauseHint\": \"Assignment logic failure\",\n" + "  \"impact\": \"PR ownership unclear\",\n"
        + "  \"suggestedAction\": \"Fix assignment mapping\"\n" + "}\n" + "\n" + "Example 5:\n" + "Input:\n"
        + "\"need PR with highest items count for SAP\"\n" + "\n" + "Output:\n" + "{\n" + "  \"category\": \"OTHER\",\n"
        + "  \"severity\": \"LOW\",\n" + "  \"summary\": \"Request for PR data analysis\",\n"
        + "  \"tags\": [\"PR\", \"data request\"],\n" + "  \"subCategory\": \"Information request\",\n"
        + "  \"rootCauseHint\": \"N/A\",\n" + "  \"impact\": \"No system issue\",\n"
        + "  \"suggestedAction\": \"Provide requested data\"\n" + "}\n" + "\n"
        + "----------------------------------------\n" + "\uD83D\uDD39 OUTPUT FORMAT (STRICT)\n"
        + "----------------------------------------\n" + "\n" + "Return ONLY a JSON array:\n" + "\n" + "[\n" + "  {\n"
        + "    \"issueId\": \"123\",\n" + "    \"category\": \"INVENTORY\",\n" + "    \"severity\": \"HIGH\",\n"
        + "    \"summary\": \"...\",\n" + "    \"tags\": [\"...\"],\n" + "    \"subCategory\": \"...\",\n"
        + "    \"rootCauseHint\": \"...\",\n" + "    \"impact\": \"...\",\n" + "    \"suggestedAction\": \"...\"\n"
        + "  }\n" + "]\n" + "\n" + "NO markdown  \n" + "NO explanations  \n" + "NO extra fields  \n" + "\n"
        + "----------------------------------------\n" + "\uD83D\uDD39 INPUT\n"
        + "----------------------------------------" + inputJson;
  }


  private Map<String, IssueAIResult> parseGeminiResponse(String response, Map<String, String> pendingIssueMap) {
    log.info("RAW GEMINI RESPONSE:\n" + response);
    Map<String, IssueAIResult> resultMap = new LinkedHashMap<>();
    if (response == null || response.trim().isEmpty()) {
      return fallbackResults(pendingIssueMap);
    }
    try {
      JsonNode root = objectMapper.readTree(extractJsonPayload(response));
      if (root.isArray()) {
        for (JsonNode item : root) {

          String issueId = textValue(item, "issueId");
          if (issueId == null || !pendingIssueMap.containsKey(issueId)) {
            continue;
          }
          IssueAIResult ai = new IssueAIResult();
          ai.setIssueId(issueId);
          ai.setCategory(normalizeCategory(textValue(item, "category")));
          ai.setSeverity(normalizeSeverity(textValue(item, "severity")));
          ai.setSummary(textValue(item, "summary"));
          ai.setSubCategory(textValue(item, "subCategory"));
          ai.setRootCauseHint(textValue(item, "rootCauseHint"));
          ai.setImpact(textValue(item, "impact"));
          ai.setSuggestedAction(textValue(item, "suggestedAction"));

          if (item.has("tags") && item.get("tags").isArray()) {
            List<String> tags = new ArrayList<>();
            item.get("tags").forEach(t -> tags.add(t.asText()));
            ai.setTags(tags);
          }

          resultMap.put(issueId, ai);
        }
      }

    } catch (Exception e) {
      return fallbackResults(pendingIssueMap);
    }
    for (String issueId : pendingIssueMap.keySet()) {
      resultMap.putIfAbsent(issueId, defaultResult(issueId));
    }
    return resultMap;
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

  private Map<String, IssueAIResult> fallbackResults(Map<String, String> pendingIssueMap) {
    Map<String, IssueAIResult> fallback = new LinkedHashMap<>();
    for (String id : pendingIssueMap.keySet()) {
      fallback.put(id, defaultResult(id));
    }
    return fallback;
  }

  private IssueAIResult defaultResult(String issueId) {
    IssueAIResult def = new IssueAIResult();
    def.setIssueId(issueId);
    def.setCategory("OTHER");
    def.setSeverity("LOW");
    def.setSummary("Unable to classify");
    def.setRootCauseHint("UNKNOWN");
    def.setImpact("UNKNOWN");
    def.setSuggestedAction("Manual review required");
    def.setTags(List.of());
    return def;
  }

  private String textValue(JsonNode node, String fieldName) {
    JsonNode field = node.get(fieldName);
    return field == null || field.isNull() ? null : field.asText();
  }

  private String normalizeSeverity(String severity) {
    if (severity == null)
      return "LOW";
    switch (severity.toUpperCase()) {
      case "LOW":
      case "MEDIUM":
      case "HIGH":
      case "CRITICAL":
        return severity.toUpperCase();
      default:
        return "LOW";
    }
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
