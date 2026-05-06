package com.example.Issue.Dashboard.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.Issue.Dashboard.DTO.TeamsMessage;
import com.example.Issue.Dashboard.Repository.Issue;
import com.example.Issue.Dashboard.Repository.IssueRepository;

@Service
public class IssueService {

  @Autowired
  private IssueRepository issueRepository;

  @Autowired
  private IssueClassificationService issueClassificationService;

  public Issue processTeamsMessage(TeamsMessage message) {
    if (issueRepository.existsByMessageId(message.getMessageId())) {
      return issueRepository.findByMessageId(message.getMessageId());
    }

    Issue issue = new Issue();
    issue.setMessageId(message.getMessageId());
    issue.setMessageTitle(message.getMessageTitle());
    issue.setText(message.getText());
    issue.setUsername(message.getUser());
    issue.setTimestamp(parseTimestamp(message.getTimestamp()));
    issue.setSource("MS Teams");
    issue.setProcessed(false);
    return issueRepository.save(issue);
  }

  public boolean getUnprocessedIssues() {
    List<Issue> unprocessedIssues = issueRepository.findByProcessedFalse();
    if (unprocessedIssues.isEmpty()) {
      return true;
    }
    try {
      issueClassificationService.classifyPendingIssues(unprocessedIssues);
    } catch (IOException e) {
      return false;
    }
    return true;
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