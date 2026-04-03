package com.assistly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import com.assistly.model.Role;
import com.assistly.model.Community;
import com.assistly.model.Event;
import com.assistly.model.Meeting;
import com.assistly.model.Rule;
import com.assistly.repository.UserRepository;
import com.assistly.repository.CommunityRepository;
import com.assistly.repository.EventRepository;
import com.assistly.repository.MeetingRepository;
import com.assistly.repository.RuleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;

@SpringBootApplication
public class AssistlyApplication {

	public static void main(String[] args) {
		SpringApplication.run(AssistlyApplication.class, args);
	}

	@Bean
	public CommandLineRunner runMigration(JdbcTemplate jdbcTemplate, UserRepository userRepository, 
                                          CommunityRepository communityRepository, EventRepository eventRepository,
                                          MeetingRepository meetingRepository, RuleRepository ruleRepository,
                                          PasswordEncoder passwordEncoder) {
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

			// Seeding Pulse Content
			if (eventRepository.findByCommunityId(globalComm.getId()).isEmpty()) {
				Event e1 = new Event();
				e1.setTitle("Syndicate Orientation");
				e1.setDescription("Tactical briefing for new residents and volunteers.");
				e1.setLocation("Virtual Hub");
				e1.setStartTime(LocalDateTime.now().plusDays(2));
				e1.setCommunity(globalComm);
				eventRepository.save(e1);
			}

			if (meetingRepository.findByCommunityId(globalComm.getId()).isEmpty()) {
				Meeting m1 = new Meeting();
				m1.setTitle("Weekly Strategy Council");
				m1.setLink("https://meet.assistly.matrix");
				m1.setMeetingTime(LocalDateTime.now().plusDays(1));
				m1.setCommunity(globalComm);
				meetingRepository.save(m1);
			}

			if (ruleRepository.findByCommunityId(globalComm.getId()).isEmpty()) {
				Rule r1 = new Rule();
				r1.setDescription("Respect individual privacy protocols during tactical broadcasts.");
				r1.setCommunity(globalComm);
				ruleRepository.save(r1);

				Rule r2 = new Rule();
				r2.setDescription("Verify impact before final mission completion protocol.");
				r2.setCommunity(globalComm);
				ruleRepository.save(r2);
			}

			System.out.println("SYNC: Global Syndicate initialized with tactical pulse content.");
		};
	}
}
