package com.example.Issue.Dashboard.Service;

import java.time.LocalDateTime;

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
        issue.setText(message.getText());
        issue.setUsername(message.getUser());
        issue.setSource(message.getChannel());
        issue.setTimestamp(LocalDateTime.now());
        issue.setSource("Power Automate");
        issue.setProcessed(false);
        return issueRepository.save(issue);
    }
}