package com.assistly.controller;

import com.assistly.model.Community;
import com.assistly.model.User;
import com.assistly.repository.CommunityRepository;
import com.assistly.repository.UserRepository;
import com.assistly.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/communities")
public class CommunityController {

    @Autowired
    CommunityRepository communityRepository;

    @Autowired
    UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getAllCommunities() {
        return ResponseEntity.ok(communityRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> createCommunity(@RequestBody Community community) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            User user = userRepository.findById(userDetails.getId()).orElse(null);
            if (user != null) {
                community.setAdmin(user);
                community.getMembers().add(user); 
                Community _community = communityRepository.save(community);
                return ResponseEntity.ok(_community);
            }
        }
        return ResponseEntity.status(403).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCommunity(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Optional<Community> commData = communityRepository.findById(id);
        if (commData.isPresent() && currentUser != null) {
            Community comm = commData.get();
            if (comm.getAdmin() != null && comm.getAdmin().getId().equals(currentUser.getId())) {
                communityRepository.deleteById(id);
                return ResponseEntity.ok().build();
            }
        }
        return ResponseEntity.status(403).build();
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<?> joinCommunity(@PathVariable Long id) {
        User user = getCurrentUser();
        Optional<Community> commData = communityRepository.findById(id);

        if (user != null && commData.isPresent()) {
            Community comm = commData.get();
            if (comm.getMembers().contains(user)) return ResponseEntity.badRequest().body("Already a member");
            
            if (comm.isPrivate()) {
                comm.getPendingMembers().add(user);
                return ResponseEntity.ok(communityRepository.save(comm));
            } else {
                comm.getMembers().add(user);
                return ResponseEntity.ok(communityRepository.save(comm));
            }
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/{id}/members/{userId}/approve")
    public ResponseEntity<?> approveMember(@PathVariable Long id, @PathVariable Long userId) {
        User adminUser = getCurrentUser();
        Optional<Community> commData = communityRepository.findById(id);

        if (adminUser != null && commData.isPresent()) {
            Community comm = commData.get();
            if (comm.getAdmin() != null && comm.getAdmin().getId().equals(adminUser.getId())) {
                Optional<User> pendingUser = userRepository.findById(userId);
                if (pendingUser.isPresent() && comm.getPendingMembers().contains(pendingUser.get())) {
                    comm.getPendingMembers().remove(pendingUser.get());
                    comm.getMembers().add(pendingUser.get());
                    return ResponseEntity.ok(communityRepository.save(comm));
                }
            } else {
                return ResponseEntity.status(403).body("Only Admin can approve requests.");
            }
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/{id}/members/{userId}/reject")
    public ResponseEntity<?> rejectMember(@PathVariable Long id, @PathVariable Long userId) {
        User adminUser = getCurrentUser();
        Optional<Community> commData = communityRepository.findById(id);

        if (adminUser != null && commData.isPresent()) {
            Community comm = commData.get();
            if (comm.getAdmin() != null && comm.getAdmin().getId().equals(adminUser.getId())) {
                Optional<User> pendingUser = userRepository.findById(userId);
                if (pendingUser.isPresent() && comm.getPendingMembers().contains(pendingUser.get())) {
                    comm.getPendingMembers().remove(pendingUser.get());
                    return ResponseEntity.ok(communityRepository.save(comm));
                }
            } else {
                return ResponseEntity.status(403).body("Only Admin can reject requests.");
            }
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        User adminUser = getCurrentUser();
        Optional<Community> commData = communityRepository.findById(id);

        if (adminUser != null && commData.isPresent()) {
            Community comm = commData.get();
            if (comm.getAdmin() != null && comm.getAdmin().getId().equals(adminUser.getId())) {
                Optional<User> memberUser = userRepository.findById(userId);
                if (memberUser.isPresent() && comm.getMembers().contains(memberUser.get())) {
                    if (comm.getAdmin().getId().equals(userId)) return ResponseEntity.badRequest().body("Admin cannot remove themselves");
                    comm.getMembers().remove(memberUser.get());
                    return ResponseEntity.ok(communityRepository.save(comm));
                }
            } else {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.badRequest().build();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            return userRepository.findById(userDetails.getId()).orElse(null);
        }
        return null;
    }
}
