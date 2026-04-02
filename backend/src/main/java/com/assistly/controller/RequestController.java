package com.assistly.controller;

import com.assistly.model.Request;
import com.assistly.model.RequestStatus;
import com.assistly.model.User;
import com.assistly.repository.RequestRepository;
import com.assistly.repository.UserRepository;
import com.assistly.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/requests")
public class RequestController {

    @Autowired
    RequestRepository requestRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    com.assistly.repository.CommunityRepository communityRepository;

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getAllRequests() {
        return ResponseEntity.ok(requestRepository.findAll());
    }

    @GetMapping("/community/{communityId}")
    public ResponseEntity<?> getCommunityRequests(@PathVariable Long communityId) {
        User currentUser = getCurrentUser();
        Optional<com.assistly.model.Community> commData = communityRepository.findById(communityId);
        
        if (currentUser != null && commData.isPresent()) {
            com.assistly.model.Community comm = commData.get();
            if (comm.getMembers().contains(currentUser) || (comm.getAdmin() != null && comm.getAdmin().getId().equals(currentUser.getId()))) {
                return ResponseEntity.ok(requestRepository.findByCommunity(comm));
            }
        }
        return ResponseEntity.status(403).build();
    }

    @PostMapping("/community/{communityId}")
    public ResponseEntity<?> createRequest(@PathVariable Long communityId, @RequestBody Request requestBody) {
        User currentUser = getCurrentUser();
        Optional<com.assistly.model.Community> commData = communityRepository.findById(communityId);

        if (currentUser != null && commData.isPresent()) {
            com.assistly.model.Community comm = commData.get();
            if (comm.getMembers().contains(currentUser) || (comm.getAdmin() != null && comm.getAdmin().getId().equals(currentUser.getId()))) {
                requestBody.setAuthor(currentUser);
                requestBody.setStatus(RequestStatus.OPEN);
                requestBody.setCommunity(comm);
                Request savedRequest = requestRepository.save(requestBody);
                return ResponseEntity.ok(savedRequest);
            }
        }
        return ResponseEntity.status(403).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRequest(@PathVariable Long id, @RequestBody Request requestDetails) {
        Optional<Request> reqData = requestRepository.findById(id);

        if (reqData.isPresent()) {
            Request _request = reqData.get();
            _request.setTitle(requestDetails.getTitle());
            _request.setDescription(requestDetails.getDescription());
            _request.setLatitude(requestDetails.getLatitude());
            _request.setLongitude(requestDetails.getLongitude());
            return ResponseEntity.ok(requestRepository.save(_request));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRequest(@PathVariable Long id) {
        try {
            requestRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Optional<Request> reqData = requestRepository.findById(id);

        if (reqData.isPresent() && currentUser != null) {
            Request _request = reqData.get();
            if (_request.getAuthor().getId().equals(currentUser.getId())) {
                return ResponseEntity.badRequest().body("Cannot accept own request");
            }
            _request.setVolunteer(currentUser);
            _request.setStatus(RequestStatus.IN_PROGRESS);
            return ResponseEntity.ok(requestRepository.save(_request));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submitForVerification(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Optional<Request> reqData = requestRepository.findById(id);

        if (reqData.isPresent() && currentUser != null) {
            Request _request = reqData.get();
            boolean isAssignedVolunteer = _request.getVolunteer() != null && _request.getVolunteer().getId().equals(currentUser.getId());
            
            if (isAssignedVolunteer && _request.getStatus() == RequestStatus.IN_PROGRESS) {
                _request.setStatus(RequestStatus.PENDING_VERIFICATION);
                return ResponseEntity.ok(requestRepository.save(_request));
            } else {
                return ResponseEntity.status(403).body("Only the active volunteer can submit this task for review.");
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeRequest(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Optional<Request> reqData = requestRepository.findById(id);

        if (reqData.isPresent() && currentUser != null) {
            Request _request = reqData.get();
            boolean isAuthor = _request.getAuthor().getId().equals(currentUser.getId());
            boolean isAdmin = "ADMIN".equals(currentUser.getRole().name());
            
            if ((isAuthor || isAdmin) && _request.getStatus() == RequestStatus.PENDING_VERIFICATION) {
                _request.setStatus(RequestStatus.COMPLETED);
                return ResponseEntity.ok(requestRepository.save(_request));
            } else {
                return ResponseEntity.status(403).body("Only the resident can officially verify and complete this task.");
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectSubmission(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Optional<Request> reqData = requestRepository.findById(id);

        if (reqData.isPresent() && currentUser != null) {
            Request _request = reqData.get();
            boolean isAuthor = _request.getAuthor().getId().equals(currentUser.getId());
            boolean isAdmin = "ADMIN".equals(currentUser.getRole().name());
            
            if ((isAuthor || isAdmin) && _request.getStatus() == RequestStatus.PENDING_VERIFICATION) {
                _request.setStatus(RequestStatus.IN_PROGRESS);
                return ResponseEntity.ok(requestRepository.save(_request));
            } else {
                return ResponseEntity.status(403).body("Only the resident can reject this verification.");
            }
        }
        return ResponseEntity.notFound().build();
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
