package com.kisansarthi.auth;

import java.util.Optional;

public class SecurityUserContext {

    private final String username;
    private final Role role;
    private final String mandiId;
    private final String district;

    public SecurityUserContext(String username, Role role, String mandiId, String district) {
        this.username = username;
        this.role = role;
        this.mandiId = mandiId;
        this.district = district;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public String getMandiId() {
        return mandiId;
    }

    public String getDistrict() {
        return district;
    }

    public boolean isAdmin() {
        return role == Role.ROLE_ADMIN;
    }

    public boolean isDistrictOfficer() {
        return role == Role.ROLE_DISTRICT_OFFICER;
    }

    public boolean isMandiScoped() {
        return role == Role.ROLE_MANDI_MANAGER || role == Role.ROLE_MANDI_OPERATOR;
    }
}
