package com.example.Issue.Dashboard.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface SharePointMessageRepository extends MongoRepository<SharePointMessage, String> {
    
    Optional<SharePointMessage> findBySharePointItemId(String sharePointItemId);
    
    List<SharePointMessage> findByRetrievedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<SharePointMessage> findByCategorized(boolean categorized);
    
    List<SharePointMessage> findByCategory(String category);
    
    @Query("{'messageText': {$regex: ?0, $options: 'i'}}")
    List<SharePointMessage> findByMessageTextContaining(String text);
    
    long countByCategorized(boolean categorized);
}