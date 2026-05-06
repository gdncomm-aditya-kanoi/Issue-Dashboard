package com.example.Issue.Dashboard.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface IssueRepository extends MongoRepository<Issue, String> {

  List<Issue> findByProcessedFalse();
}