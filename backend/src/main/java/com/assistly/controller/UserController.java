package com.assistly.controller;

import com.assistly.model.User;
import com.assistly.model.RequestStatus;
import com.assistly.repository.UserRepository;
import com.assistly.repository.RequestRepository;
import com.assistly.security.services.UserDetailsImpl;
import com.assistly.payload.response.UserProfileDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RequestRepository requestRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();                 
            Optional<User> userData = userRepository.findById(userDetails.getId());
            if (userData.isPresent()) {
                User user = userData.get();
                Long posted = requestRepository.countByAuthor(user);
                Long completed = requestRepository.countByVolunteerAndStatus(user, RequestStatus.COMPLETED);
                
                List<String> achievements = new ArrayList<>();
                if (completed >= 1) achievements.add("First Drop");
                if (completed >= 5) achievements.add("Silver Helper");
                if (completed >= 20) achievements.add("Gold Samaritan");
                if (posted >= 5) achievements.add("Active Citizen");

                Map<String, Long> stats = new HashMap<>();
                stats.put("requestsPosted", posted);
                stats.put("requestsCompleted", completed);

                UserProfileDTO profile = new UserProfileDTO(
                    user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.isVolunteer(), stats, achievements
                );
                return ResponseEntity.ok(profile);
            }
        }
        return ResponseEntity.status(403).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserProfile(@PathVariable Long id, @RequestBody User newDetails) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
             UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
             if (userDetails.getId().equals(id)) {
                 Optional<User> userData = userRepository.findById(id);
                 if (userData.isPresent()) {
                     User user = userData.get();
                     if (newDetails.getName() != null && !newDetails.getName().isEmpty()) user.setName(newDetails.getName());
                     if (newDetails.getEmail() != null && !newDetails.getEmail().isEmpty()) user.setEmail(newDetails.getEmail());
                     return ResponseEntity.ok(userRepository.save(user));
                 }
             }
        }
        return ResponseEntity.status(403).build();
    }

    @PutMapping("/{id}/toggle-mode")
    public ResponseEntity<?> toggleUserMode(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            if (userDetails.getId().equals(id)) {
                Optional<User> userData = userRepository.findById(id);
                if (userData.isPresent()) {
                    User user = userData.get();
                    user.setVolunteer(!user.isVolunteer());
                    userRepository.save(user);
                    return ResponseEntity.ok(user);
                }
            }
        }
        return ResponseEntity.status(403).body("Unauthorized to modify this user");
    }
}
