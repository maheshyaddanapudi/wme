package com.workday.pwe.dto;

import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for user group information.
 */
public class UserGroupDTO {
    private UUID id;
    private String name;
    private String description;
    private List<UserDTO> members;
    
    // Default constructor
    public UserGroupDTO() {
    }
    
    // Constructor with required fields
    public UserGroupDTO(UUID id, String name) {
        this.id = id;
        this.name = name;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<UserDTO> getMembers() {
        return members;
    }
    
    public void setMembers(List<UserDTO> members) {
        this.members = members;
    }
    
    // Utility methods
    public void addMember(UserDTO user) {
        if (members != null) {
            members.add(user);
        }
    }
    
    public boolean containsUser(UUID userId) {
        if (members == null) {
            return false;
        }
        
        return members.stream()
            .anyMatch(user -> user.getId().equals(userId));
    }
}