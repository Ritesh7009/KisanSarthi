package com.kisansarthi.farmer;

import com.kisansarthi.auth.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "farmers")
public class Farmer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(name = "kisan_id", nullable = false, unique = true, length = 50)
    private String kisanId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "hindi_name", length = 150)
    private String hindiName;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "masked_aadhar", nullable = false, length = 20)
    private String maskedAadhar;

    @Column(name = "aadhar_hash", length = 64)
    private String aadharHash;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String village;

    @Column(name = "land_size_acres", nullable = false)
    private BigDecimal landSizeAcres = BigDecimal.ZERO;

    @Column(name = "bank_account_last4", length = 10)
    private String bankAccountLast4;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Farmer() {}

    public Farmer(String kisanId, String name, String phone, String district, String village, BigDecimal landSizeAcres, String maskedAadhar) {
        this.kisanId = kisanId;
        this.name = name;
        this.phone = phone;
        this.district = district;
        this.village = village;
        this.landSizeAcres = landSizeAcres;
        this.maskedAadhar = maskedAadhar;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getKisanId() { return kisanId; }
    public void setKisanId(String kisanId) { this.kisanId = kisanId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHindiName() { return hindiName; }
    public void setHindiName(String hindiName) { this.hindiName = hindiName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getMaskedAadhar() { return maskedAadhar; }
    public void setMaskedAadhar(String maskedAadhar) { this.maskedAadhar = maskedAadhar; }
    public String getAadharHash() { return aadharHash; }
    public void setAadharHash(String aadharHash) { this.aadharHash = aadharHash; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }
    public BigDecimal getLandSizeAcres() { return landSizeAcres; }
    public void setLandSizeAcres(BigDecimal landSizeAcres) { this.landSizeAcres = landSizeAcres; }
    public String getBankAccountLast4() { return bankAccountLast4; }
    public void setBankAccountLast4(String bankAccountLast4) { this.bankAccountLast4 = bankAccountLast4; }
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
