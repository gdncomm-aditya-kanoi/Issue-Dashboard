package com.example.Issue.Dashboard;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Issue.Dashboard.DTO.TeamsMessage;
import com.example.Issue.Dashboard.Repository.Issue;
import com.example.Issue.Dashboard.Repository.IssueRepository;

@RestController
@RequestMapping("/teams")
public class TeamsWebhookController {

  @Autowired
  private IssueRepository issueRepository;

  @PostMapping("/webhook")
  public ResponseEntity<String> receiveMessage(@RequestBody TeamsMessage message) {
    String category = categorize(message.getText());
    Issue issue = new Issue();
    issue.setText(message.getText());
    issue.setUsername(message.getUser());
    issue.setCategory(category);
    issue.setTimestamp(OffsetDateTime.parse(message.getTimestamp()).toLocalDateTime());
    issueRepository.save(issue);
    System.out.println("Saved Issue: " + issue.getText());
    return ResponseEntity.ok("Saved");
  }

  private String categorize(String text) {
    if (text == null)
      return "OTHER";
    text = text.toLowerCase();
    if (text.contains("api") || text.contains("server") || text.contains("backend"))
      return "BACKEND";
    if (text.contains("ui") || text.contains("button") || text.contains("frontend"))
      return "FRONTEND";
    if (text.contains("timeout") || text.contains("down") || text.contains("infra"))
      return "INFRA";
    if (text.contains("data") || text.contains("wrong") || text.contains("mismatch"))
      return "DATA";
    if (text.contains("access") || text.contains("permission") || text.contains("role"))
      return "ACCESS";
    return "OTHER";
  }

  @GetMapping("/summary")
  public Map<String, Long> getSummary() {
    return issueRepository.findAll().stream().collect(Collectors.groupingBy(Issue::getCategory, Collectors.counting()));
  }

  @GetMapping("/issues")
  public List<Issue> getAllIssues() {
    return issueRepository.findAll();
  }

  @GetMapping("/issues/today")
  public List<Issue> todayIssues() {
    LocalDate today = LocalDate.now();
    return issueRepository.findAll().stream().filter(i -> i.getTimestamp().toLocalDate().equals(today)).toList();
  }

}