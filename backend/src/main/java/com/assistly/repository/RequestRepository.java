package com.assistly.repository;

import com.assistly.model.Request;
import com.assistly.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByAuthor(User author);
    List<Request> findByVolunteer(User volunteer);
    List<Request> findByAuthorNot(User user);
    Long countByAuthor(User author);
    Long countByVolunteerAndStatus(User volunteer, com.assistly.model.RequestStatus status);
    List<Request> findByCommunity(com.assistly.model.Community community);
    List<Request> findByCommunityId(Long communityId);
}
