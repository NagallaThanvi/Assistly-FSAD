package com.assistly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class AssistlyApplication {

	public static void main(String[] args) {
		SpringApplication.run(AssistlyApplication.class, args);
	}

	@Bean
	public CommandLineRunner runMigration(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				jdbcTemplate.execute("ALTER TABLE requests MODIFY COLUMN status VARCHAR(255)");
				System.out.println("Migrated status column to VARCHAR(255) successfully.");
			} catch (Exception e) {
				System.out.println("Status column already migrated or MySQL bypass triggered.");
			}
		};
	}
}
