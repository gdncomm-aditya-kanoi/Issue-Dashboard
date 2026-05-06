package com.example.Issue.Dashboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.Issue.Dashboard.DTO.TeamsMessage;
import com.example.Issue.Dashboard.Repository.Issue;
import com.example.Issue.Dashboard.Service.IssueClassificationService;
import com.example.Issue.Dashboard.Service.IssueService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(TeamsWebhookController.class)
class TeamsWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IssueService issueService;

    @MockBean
    private IssueClassificationService issueClassificationService;

    @Test
    void webhookEndpointReturnsSavedIssue() throws Exception {
        TeamsMessage message = new TeamsMessage();
        message.setText("Payments API returns 500 when saving an order");
        message.setUser("alice");
        message.setChannel("general");
        message.setTimestamp("2026-05-06T16:45:00Z");

        Issue savedIssue = new Issue();
        savedIssue.setId("issue-1");
        savedIssue.setText(message.getText());
        savedIssue.setUsername(message.getUser());
        savedIssue.setSource("general");
        savedIssue.setProcessed(false);

        when(issueService.processTeamsMessage(any(TeamsMessage.class))).thenReturn(savedIssue);

        mockMvc.perform(post("/api/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("issue-1"))
                .andExpect(jsonPath("$.text").value("Payments API returns 500 when saving an order"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.source").value("general"))
                .andExpect(jsonPath("$.processed").value(false));

        verify(issueService).processTeamsMessage(any(TeamsMessage.class));
    }

    @Test
    void getUnprocessedIssuesEndpointReturnsIssueMap() throws Exception {
        when(issueService.getUnprocessedIssues()).thenReturn(Map.of("issue-2", "Frontend button overlaps on mobile"));

        mockMvc.perform(get("/api/get-unprocessed-issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issue-2").value("Frontend button overlaps on mobile"));

        verify(issueService).getUnprocessedIssues();
    }

    @Test
    void classifyPendingIssuesEndpointReturnsGeminiCategories() throws Exception {
        when(issueClassificationService.classifyPendingIssues()).thenReturn(Map.of("issue-3", "BACKEND"));

        mockMvc.perform(post("/api/classify-pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issue-3").value("BACKEND"));

        verify(issueClassificationService).classifyPendingIssues();
    }
}
