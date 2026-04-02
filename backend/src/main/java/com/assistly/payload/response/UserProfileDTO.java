package com.assistly.payload.response;

import java.util.List;
import java.util.Map;

public class UserProfileDTO {
    private Long id;
    private String name;
    private String email;
    private String role;
    private boolean isVolunteer;
    private Map<String, Long> stats;
    private List<String> achievements;

    public UserProfileDTO() {}

    public UserProfileDTO(Long id, String name, String email, String role, boolean isVolunteer, Map<String, Long> stats, List<String> achievements) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.isVolunteer = isVolunteer;
        this.stats = stats;
        this.achievements = achievements;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isVolunteer() { return isVolunteer; }
    public void setVolunteer(boolean isVolunteer) { this.isVolunteer = isVolunteer; }
    public Map<String, Long> getStats() { return stats; }
    public void setStats(Map<String, Long> stats) { this.stats = stats; }
    public List<String> getAchievements() { return achievements; }
    public void setAchievements(List<String> achievements) { this.achievements = achievements; }
}
