package com.assistly.services;

import com.assistly.model.Request;
import com.assistly.model.User;
import com.assistly.payload.request.RequestDto;
import java.util.List;

public interface RequestService {
    List<Request> getAllRequests();
    Request createRequest(Long communityId, RequestDto requestDto, User currentUser);
    List<Request> getRequestsByCommunity(Long communityId);
    Request getRequestById(Long id);
    Request updateRequest(Long id, RequestDto requestDto, User currentUser);
    void deleteRequest(Long id, User currentUser);
    
    // Status Transitions
    Request acceptRequest(Long id, User currentUser);
    Request submitForVerification(Long id, User currentUser);
    Request completeRequest(Long id, User currentUser);
    Request rejectSubmission(Long id, User currentUser);
}
