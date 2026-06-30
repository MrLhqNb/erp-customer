package com.jumbosoft.erpcustomer.model.dto.response;

import com.jumbosoft.erpcustomer.model.entity.User;

public class UserResponse {

    private Long id;
    private String username;
    private String role;
    private String nickname;

    public static UserResponse from(User user) {
        UserResponse r = new UserResponse();
        r.id = user.getId();
        r.username = user.getUsername();
        r.role = user.getRole();
        r.nickname = user.getNickname();
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
