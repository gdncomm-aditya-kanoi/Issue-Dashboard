package com.example.Issue.Dashboard.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Issue.Dashboard.DTO.TeamsMessage;
import com.example.Issue.Dashboard.Repository.Issue;
import com.example.Issue.Dashboard.Service.IssueClassificationService;
import com.example.Issue.Dashboard.Service.IssueService;

@RestController
@RequestMapping("/api")
public class TeamsWebhookController {

    @Autowired
    private IssueService issueService;

    @Autowired
    private IssueClassificationService issueClassificationService;

    @PostMapping("/webhook")
    public ResponseEntity<Issue> receiveMessage(@RequestBody TeamsMessage message) {
        Issue savedIssue = issueService.processTeamsMessage(message);
        return ResponseEntity.ok(savedIssue);
    }

    @GetMapping("/get-unprocessed-issues")
    public Map<String, String> getUnprocessedIssues() {
        return issueService.getUnprocessedIssues();
    }

    @PostMapping("/classify-pending")
    public ResponseEntity<Map<String, String>> classifyPendingIssues() {
        return ResponseEntity.ok(issueClassificationService.classifyPendingIssues());
    }
    
}