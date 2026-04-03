package com.assistly.controller;

import com.assistly.model.Event;
import com.assistly.model.Meeting;
import com.assistly.model.Rule;
import com.assistly.repository.EventRepository;
import com.assistly.repository.MeetingRepository;
import com.assistly.repository.RuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/content")
public class ContentController {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private RuleRepository ruleRepository;

    @GetMapping("/pulse/{communityId}")
    public ResponseEntity<?> getSyndicatePulse(@PathVariable Long communityId) {
        Map<String, Object> pulse = new HashMap<>();
        
        List<Event> events = eventRepository.findByCommunityId(communityId);
        List<Meeting> meetings = meetingRepository.findByCommunityId(communityId);
        List<Rule> rules = ruleRepository.findByCommunityId(communityId);
        
        pulse.put("events", events);
        pulse.put("meetings", meetings);
        pulse.put("rules", rules);
        
        return ResponseEntity.ok(pulse);
    }
}
