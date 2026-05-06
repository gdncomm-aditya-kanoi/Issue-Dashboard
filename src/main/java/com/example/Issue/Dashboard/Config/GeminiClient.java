package com.example.Issue.Dashboard.Config;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class GeminiClient {

    @Value("${gemini-client-api-key:}")
    private String apiKey;

    @Value("${gemini-client-api-url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent}")
    private String apiUrl;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private final Gson gson = new Gson();

    public String generateContent(String prompt) throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty() || "YOUR_GEMINI_API_KEY_HERE".equals(apiKey)) {
            throw new IOException("Gemini API key is not configured. Please set GEMINI_CLIENT_API_KEY.");
        }

        String requestBodyJson = buildRequestBody(prompt);
        RequestBody body = RequestBody.create(requestBodyJson, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(apiUrl + "?key=" + apiKey)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + " with body: "
                        + (response.body() != null ? response.body().string() : "null"));
            }
            return response.body() != null ? parseResponse(response.body().string()) : "No response";
        }
    }

    private String buildRequestBody(String prompt) {
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(textPart);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.0);
        generationConfig.addProperty("topP", 0.95);
        generationConfig.addProperty("maxOutputTokens", 8192);
        generationConfig.addProperty("candidateCount", 1);
        generationConfig.addProperty("responseMimeType", "application/json");

        JsonObject root = new JsonObject();
        root.add("contents", contents);
        root.add("generationConfig", generationConfig);

        return gson.toJson(root);
    }

    private String parseResponse(String responseBody) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            return jsonResponse.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            System.err.println("Failed to parse Gemini response: " + responseBody);
            return "Error parsing response.";
        }
    }
}
