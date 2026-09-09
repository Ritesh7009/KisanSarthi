package com.kisansarthi.auth;

import java.util.UUID;

public class UserDto {
    private UUID id;
    private String username;
    private String phone;
    private String role;
    private String mandiId;
    private String name;
    private String district;
    private String village;
    private String maskedAadhar;

    public UserDto() {}

    public UserDto(UUID id, String username, String phone, String role, String mandiId) {
        this.id = id;
        this.username = username;
        this.phone = phone;
        this.role = role;
        this.mandiId = mandiId;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getMandiId() { return mandiId; }
    public void setMandiId(String mandiId) { this.mandiId = mandiId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }
    public String getMaskedAadhar() { return maskedAadhar; }
    public void setMaskedAadhar(String maskedAadhar) { this.maskedAadhar = maskedAadhar; }
}
