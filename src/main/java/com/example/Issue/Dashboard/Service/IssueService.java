package com.example.Issue.Dashboard.Service;

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

    public Issue processTeamsMessage(TeamsMessage message) {
        Issue issue = new Issue();
        issue.setMessageId(message.getMessageId());
        issue.setMessageTitle(message.getMessageTitle());
        issue.setText(message.getText());
        issue.setUsername(message.getUser());
        issue.setSource(message.getChannel());
        issue.setTimestamp(LocalDateTime.now());
        issue.setSource("MS Teams");
        issue.setProcessed(false);
        return issueRepository.save(issue);
    }

    public Map<String, String> getUnprocessedIssues() {
        List<Issue> unprocessedIssues = issueRepository.findByProcessedFalse();
        Map<String, String> issueMap = new HashMap<>();
        for (Issue issue : unprocessedIssues) {
            issueMap.put(issue.getMessageId(), issue.getText());
        }
        return issueMap;
    }
}