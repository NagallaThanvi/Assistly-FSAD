package com.assistly.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.assistly.model.Request;
import com.assistly.model.User;
import com.assistly.repository.UserRepository;
import com.assistly.security.services.UserDetailsImpl;
import com.assistly.services.RequestService;
import com.assistly.payload.request.RequestDto;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/requests")
public class RequestController {

    @Autowired
    RequestService requestService;

    @Autowired
    UserRepository userRepository;

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getAllRequests() {
        return ResponseEntity.ok(requestService.getAllRequests());
    }

    @GetMapping("/community/{communityId}")
    public ResponseEntity<?> getCommunityRequests(@PathVariable Long communityId) {
        try {
            return ResponseEntity.ok(requestService.getRequestsByCommunity(communityId));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping("/community/{communityId}")
    public ResponseEntity<?> createRequest(@PathVariable Long communityId, @RequestBody RequestDto requestDto) {
        User currentUser = getCurrentUser();
        try {
            Request savedRequest = requestService.createRequest(communityId, requestDto, currentUser);
            return ResponseEntity.ok(savedRequest);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRequest(@PathVariable Long id, @RequestBody RequestDto requestDto) {
        User currentUser = getCurrentUser();
        try {
            Request updated = requestService.updateRequest(id, requestDto, currentUser);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRequest(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        try {
            requestService.deleteRequest(id, currentUser);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        try {
            Request accepted = requestService.acceptRequest(id, currentUser);
            return ResponseEntity.ok(accepted);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submitForVerification(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        try {
            Request submitted = requestService.submitForVerification(id, currentUser);
            return ResponseEntity.ok(submitted);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeRequest(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        try {
            Request completed = requestService.completeRequest(id, currentUser);
            return ResponseEntity.ok(completed);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectSubmission(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        try {
            Request rejected = requestService.rejectSubmission(id, currentUser);
            return ResponseEntity.ok(rejected);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
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
