package com.example.Issue.Dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.Issue.Dashboard.Config.GeminiClient;
import com.example.Issue.Dashboard.Repository.Issue;
import com.example.Issue.Dashboard.Repository.IssueRepository;
import com.example.Issue.Dashboard.Service.IssueClassificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class IssueClassificationServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private GeminiClient geminiClient;

    private IssueClassificationService issueClassificationService;

    @BeforeEach
    void setUp() {
        issueClassificationService = new IssueClassificationService(issueRepository, geminiClient, new ObjectMapper());
    }

    @Test
    void classifyPendingIssuesPersistsGeminiCategories() throws Exception {
        Issue firstIssue = new Issue();
        firstIssue.setId("issue-1");
        firstIssue.setText("Payments API returns 500");
        firstIssue.setProcessed(false);

        Issue secondIssue = new Issue();
        secondIssue.setId("issue-2");
        secondIssue.setText("Frontend button overlaps on mobile");
        secondIssue.setProcessed(false);

        when(issueRepository.findByProcessedFalse()).thenReturn(Arrays.asList(firstIssue, secondIssue));
        when(geminiClient.generateContent(anyString())).thenReturn(
                "[{\"issueId\":\"issue-1\",\"category\":\"backend\"},{\"issueId\":\"issue-2\",\"category\":\"FRONTEND\"}]");

        Map<String, String> categories = issueClassificationService.classifyPendingIssues();

        assertEquals("BACKEND", categories.get("issue-1"));
        assertEquals("FRONTEND", categories.get("issue-2"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Issue>> savedIssuesCaptor = ArgumentCaptor.forClass(List.class);
        verify(issueRepository).saveAll(savedIssuesCaptor.capture());

        List<Issue> savedIssues = savedIssuesCaptor.getValue();
        assertEquals(2, savedIssues.size());
        assertTrue(savedIssues.stream().allMatch(Issue::isProcessed));
        assertEquals("BACKEND", savedIssues.get(0).getCategory());
        assertEquals("FRONTEND", savedIssues.get(1).getCategory());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generateContent(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Payments API returns 500"));
        assertTrue(prompt.contains("Frontend button overlaps on mobile"));
        assertTrue(prompt.contains("Return only a valid JSON array"));
    }

    @Test
    void classifyPendingIssuesFallsBackToOtherWhenGeminiFails() throws Exception {
        Issue issue = new Issue();
        issue.setId("issue-9");
        issue.setText("Unknown outage reported in chat");
        issue.setProcessed(false);

        when(issueRepository.findByProcessedFalse()).thenReturn(Arrays.asList(issue));
        when(geminiClient.generateContent(anyString())).thenThrow(new IOException("Gemini unavailable"));

        Map<String, String> categories = issueClassificationService.classifyPendingIssues();

        assertEquals("OTHER", categories.get("issue-9"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Issue>> savedIssuesCaptor = ArgumentCaptor.forClass(List.class);
        verify(issueRepository).saveAll(savedIssuesCaptor.capture());

        Issue savedIssue = savedIssuesCaptor.getValue().get(0);
        assertEquals("OTHER", savedIssue.getCategory());
        assertTrue(savedIssue.isProcessed());
    }
}
