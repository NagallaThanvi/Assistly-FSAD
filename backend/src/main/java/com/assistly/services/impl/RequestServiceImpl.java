package com.assistly.services.impl;

import com.assistly.model.Request;
import com.assistly.model.User;
import com.assistly.model.Community;
import com.assistly.model.RequestStatus;
import com.assistly.repository.RequestRepository;
import com.assistly.repository.CommunityRepository;
import com.assistly.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.assistly.payload.request.RequestDto;

@Service
public class RequestServiceImpl implements RequestService {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Override
    public List<Request> getAllRequests() {
        return requestRepository.findAll();
    }

    @Override
    @Transactional
    public Request createRequest(Long communityId, RequestDto requestDto, User currentUser) {
        if (currentUser == null) {
            throw new RuntimeException("Unauthenticated user cannot create requests");
        }

        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Community not found with id: " + communityId));

        if (!hasAccess(currentUser, community)) {
            throw new RuntimeException("User does not have access to this community");
        }

        Request request = new Request();
        request.setTitle(requestDto.getTitle());
        request.setDescription(requestDto.getDescription());
        request.setLatitude(requestDto.getLatitude());
        request.setLongitude(requestDto.getLongitude());
        request.setAuthor(currentUser);
        request.setCommunity(community);
        request.setStatus(RequestStatus.OPEN);

        return requestRepository.save(request);
    }

    @Override
    public List<Request> getRequestsByCommunity(Long communityId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Community not found"));
        return requestRepository.findByCommunity(community);
    }

    @Override
    public Request getRequestById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mission Request ID " + id + " not found in syndicate records."));
    }

    @Override
    @Transactional
    public Request updateRequest(Long id, RequestDto requestDto, User currentUser) {
        Request request = getRequestById(id);
        
        // Authorization check: Only author or admin can update
        if (!request.getAuthor().getId().equals(currentUser.getId()) && !"ADMIN".equals(currentUser.getRole().name())) {
            throw new RuntimeException("Unauthorized: Access denied to modify mission parameters.");
        }

        request.setTitle(requestDto.getTitle());
        request.setDescription(requestDto.getDescription());
        request.setLatitude(requestDto.getLatitude());
        request.setLongitude(requestDto.getLongitude());
        
        return requestRepository.save(request);
    }

    @Override
    @Transactional
    public void deleteRequest(Long id, User currentUser) {
        Request request = getRequestById(id);
        if (!request.getAuthor().getId().equals(currentUser.getId()) && !"ADMIN".equals(currentUser.getRole().name())) {
            throw new RuntimeException("Unauthorized: Mission termination requires authorization.");
        }
        requestRepository.delete(request);
    }

    @Override
    @Transactional
    public Request acceptRequest(Long id, User currentUser) {
        Request request = getRequestById(id);
        
        if (request.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Conflict: Synchronizing with own mission broadcast is prohibited.");
        }
        
        if (request.getStatus() != RequestStatus.OPEN) {
            throw new RuntimeException("Conflict: Mission is no longer available for enlisting.");
        }

        request.setVolunteer(currentUser);
        request.setStatus(RequestStatus.IN_PROGRESS);
        return requestRepository.save(request);
    }

    @Override
    @Transactional
    public Request submitForVerification(Long id, User currentUser) {
        Request request = getRequestById(id);
        
        boolean isAssignedVolunteer = request.getVolunteer() != null && request.getVolunteer().getId().equals(currentUser.getId());
        
        if (isAssignedVolunteer && request.getStatus() == RequestStatus.IN_PROGRESS) {
            request.setStatus(RequestStatus.PENDING_VERIFICATION);
            return requestRepository.save(request);
        } else {
            throw new RuntimeException("Unauthorized: Only the enlisted operative can submit for validation.");
        }
    }

    @Override
    @Transactional
    public Request completeRequest(Long id, User currentUser) {
        Request request = getRequestById(id);
        
        boolean isAuthor = request.getAuthor().getId().equals(currentUser.getId());
        boolean isAdmin = "ADMIN".equals(currentUser.getRole().name());
        
        if ((isAuthor || isAdmin) && request.getStatus() == RequestStatus.PENDING_VERIFICATION) {
            request.setStatus(RequestStatus.COMPLETED);
            return requestRepository.save(request);
        } else {
            throw new RuntimeException("Unauthorized: Only the primary resident or admin can finalize this mission.");
        }
    }

    @Override
    @Transactional
    public Request rejectSubmission(Long id, User currentUser) {
        Request request = getRequestById(id);
        
        boolean isAuthor = request.getAuthor().getId().equals(currentUser.getId());
        boolean isAdmin = "ADMIN".equals(currentUser.getRole().name());
        
        if ((isAuthor || isAdmin) && request.getStatus() == RequestStatus.PENDING_VERIFICATION) {
            request.setStatus(RequestStatus.IN_PROGRESS);
            return requestRepository.save(request);
        } else {
            throw new RuntimeException("Unauthorized: Only the primary resident can intercept this validation.");
        }
    }

    private boolean hasAccess(User user, Community community) {
        if (community.getAdmin() != null && community.getAdmin().getId().equals(user.getId())) {
            return true;
        }
        return community.getMembers().stream()
                .anyMatch(member -> member.getId().equals(user.getId()));
    }
}
