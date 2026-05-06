package com.example.Issue.Dashboard.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@ConfigurationProperties(prefix = "sharepoint")
@Data
public class SharePointConfig {
    
    private String siteUrl;
    private String listName;
    private String fieldName;
    private String username;
    private String password;
    
    public String getListItemsUrl() {
        try {
            String encodedListName = URLEncoder.encode(listName, StandardCharsets.UTF_8.toString());
            return siteUrl + "/_api/web/lists/GetByTitle('" + encodedListName + "')/items";
        } catch (UnsupportedEncodingException e) {
            return siteUrl + "/_api/web/lists/GetByTitle('" + listName + "')/items";
        }
    }
}