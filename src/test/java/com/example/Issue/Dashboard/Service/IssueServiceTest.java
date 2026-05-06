package com.example.Issue.Dashboard.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.Issue.Dashboard.DTO.TeamsMessage;
import com.example.Issue.Dashboard.Repository.Issue;
import com.example.Issue.Dashboard.Repository.IssueRepository;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @InjectMocks
    private IssueService issueService;

    @Test
    void processTeamsMessageStoresParsedMetadata() {
        TeamsMessage message = new TeamsMessage();
        message.setText("Payments API returns 500");
        message.setUser("alice");
        message.setChannel("general");
        message.setTimestamp("2026-05-06T16:45:00Z");

        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Issue savedIssue = issueService.processTeamsMessage(message);

        ArgumentCaptor<Issue> issueCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(issueCaptor.capture());

        Issue capturedIssue = issueCaptor.getValue();
        assertEquals("Payments API returns 500", capturedIssue.getText());
        assertEquals("alice", capturedIssue.getUsername());
        assertEquals("general", capturedIssue.getSource());
        assertEquals(LocalDateTime.parse("2026-05-06T16:45:00"), capturedIssue.getTimestamp());
        assertFalse(capturedIssue.isProcessed());
        assertEquals(capturedIssue, savedIssue);
    }

    @Test
    void getUnprocessedIssuesReturnsIssueIdTextMap() {
        Issue issue = new Issue();
        issue.setId("issue-1");
        issue.setText("Frontend button overlaps");

        when(issueRepository.findByProcessedFalse()).thenReturn(List.of(issue));

        Map<String, String> issues = issueService.getUnprocessedIssues();

        assertEquals(Map.of("issue-1", "Frontend button overlaps"), issues);
        verify(issueRepository).findByProcessedFalse();
    }
}
