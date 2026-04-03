package com.assistly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import com.assistly.model.Role;
import com.assistly.model.User;
import com.assistly.model.Community;
import com.assistly.repository.UserRepository;
import com.assistly.repository.CommunityRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.Collections;
import java.util.HashSet;

@SpringBootApplication
public class AssistlyApplication {

	public static void main(String[] args) {
		SpringApplication.run(AssistlyApplication.class, args);
	}

	@Bean
	public CommandLineRunner runMigration(JdbcTemplate jdbcTemplate, UserRepository userRepository, CommunityRepository communityRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			try {
				jdbcTemplate.execute("ALTER TABLE requests MODIFY COLUMN status VARCHAR(255)");
				System.out.println("Migrated status column to VARCHAR(255) successfully.");
			} catch (Exception e) {
				System.out.println("Status column already migrated or MySQL bypass triggered.");
			}

			// Ensure Global Syndicate exists
			String globalCommName = "Global Syndicate";
			Community globalComm = communityRepository.findByName(globalCommName).orElseGet(() -> {
				Community newComm = new Community();
				newComm.setName(globalCommName);
				newComm.setDescription("The primary workspace for all Assistly residents and volunteers.");
				newComm.setPrivate(false);
				return communityRepository.save(newComm);
			});

			// Add all users to Global Syndicate (optional, but good for first run)
			userRepository.findAll().forEach(user -> {
				if (user.getRole() == Role.USER) {
					globalComm.getMembers().add(user);
				}
			});
			communityRepository.save(globalComm);
			System.out.println("SYNC: Global Syndicate synchronized with all available users.");
		};
	}
}
