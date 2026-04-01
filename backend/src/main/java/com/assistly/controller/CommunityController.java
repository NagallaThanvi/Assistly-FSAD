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
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createCommunity(@RequestBody Community community) {
        Community _community = communityRepository.save(community);
        return ResponseEntity.ok(_community);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteCommunity(@PathVariable Long id) {
        try {
            communityRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<?> joinCommunity(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            User user = userRepository.findById(userDetails.getId()).orElse(null);
            Optional<Community> commData = communityRepository.findById(id);

            if (user != null && commData.isPresent()) {
                Community comm = commData.get();
                comm.getMembers().add(user);
                return ResponseEntity.ok(communityRepository.save(comm));
            }
        }
        return ResponseEntity.badRequest().build();
    }
}
