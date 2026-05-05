package com.example.Issue.Dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("com.example.Issue.Dashboard.Repository")
public class IssueDashboardApplication {

	public static void main(String[] args) {
		SpringApplication.run(IssueDashboardApplication.class, args);
	}

}
