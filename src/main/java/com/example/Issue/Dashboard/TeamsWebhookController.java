package com.example.Issue.Dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.Issue.Dashboard.DTO.TeamsMessage;
import com.example.Issue.Dashboard.Repository.Issue;
import com.example.Issue.Dashboard.Repository.IssueRepository;
import com.example.Issue.Dashboard.Service.IssueClassificationService;

@RestController
@RequestMapping("/teams")
public class TeamsWebhookController {

  @Autowired
  private IssueRepository issueRepository;

  @Autowired
  private IssueClassificationService issueClassificationService;

  @PostMapping("/webhook")
  public ResponseEntity<String> receiveMessage(@RequestBody TeamsMessage message) {
    Issue issue = new Issue();
    issue.setText(message.getText());
    issue.setUsername(message.getUser());
    issue.setTimestamp(parseTimestamp(message.getTimestamp()));
    issue.setProcessed(false);
    issue.setSource("TEAMS");
    issueRepository.save(issue);
    System.out.println("Saved raw issue: " + issue.getText());
    return ResponseEntity.ok("Saved");
  }

  @PostMapping("/classify-pending")
  public ResponseEntity<?> classifyPendingIssues() {
    Map<String, String> classifiedIssues = issueClassificationService.classifyPendingIssues();

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("classifiedCount", classifiedIssues.size());
    response.put("categories", classifiedIssues);
    response.put("categorySummary", classifiedIssues.values().stream()
        .collect(Collectors.groupingBy(category -> category, Collectors.counting())));
    response.put("timestamp", LocalDateTime.now());

    return ResponseEntity.ok(response);
  }

  @GetMapping("/summary")
  public Map<String, Long> getSummary() {
    return issueRepository.findAll().stream().collect(Collectors.groupingBy(issue -> {
      String category = issue.getCategory();
      return category == null || category.trim().isEmpty() ? "UNCLASSIFIED" : category;
    }, Collectors.counting()));
  }

  @GetMapping("/issues")
  public List<Issue> getAllIssues() {
    return issueRepository.findAll();
  }

  @GetMapping("/issues/today")
  public List<Issue> todayIssues() {
    LocalDate today = LocalDate.now();
    return issueRepository.findAll().stream()
        .filter(i -> i.getTimestamp() != null && i.getTimestamp().toLocalDate().equals(today))
        .collect(Collectors.toList());
  }

  private LocalDateTime parseTimestamp(String timestamp) {
    if (timestamp == null || timestamp.trim().isEmpty()) {
      return LocalDateTime.now();
    }

    try {
      return OffsetDateTime.parse(timestamp).toLocalDateTime();
    } catch (Exception e) {
      return LocalDateTime.now();
    }
  }

}