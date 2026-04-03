package com.assistly.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meetings")
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String link;

    private LocalDateTime meetingTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "community_id")
    private Community community;

    public Meeting() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public LocalDateTime getMeetingTime() { return meetingTime; }
    public void setMeetingTime(LocalDateTime meetingTime) { this.meetingTime = meetingTime; }
    public Community getCommunity() { return community; }
    public void setCommunity(Community community) { this.community = community; }
}
