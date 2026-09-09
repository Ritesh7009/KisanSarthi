package com.kisansarthi.auth;

import com.kisansarthi.common.BusinessException;
import com.kisansarthi.common.ResourceNotFoundException;
import com.kisansarthi.farmer.Farmer;
import com.kisansarthi.farmer.FarmerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final FarmerRepository farmerRepository;
    private final OtpService otpService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            FarmerRepository farmerRepository,
            OtpService otpService,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.farmerRepository = farmerRepository;
        this.otpService = otpService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public void sendOtp(String phone) {
        otpService.generateAndSendOtp(phone);
    }

    @Transactional
    public AuthResponse verifyFarmerOtp(String phone, String otp) {
        boolean valid = otpService.verifyOtp(phone, otp);
        if (!valid) {
            throw new BusinessException("INVALID_OTP", "The OTP entered is incorrect. Please try again.");
        }

        // Find or create farmer user
        User user = userRepository.findByPhone(phone).orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername("kisan_" + phone);
            newUser.setPhone(phone);
            newUser.setRole(Role.ROLE_FARMER);
            return userRepository.save(newUser);
        });

        // Find or create farmer profile
        Farmer farmer = farmerRepository.findByPhone(phone).orElseGet(() -> {
            Farmer newFarmer = new Farmer();
            newFarmer.setUser(user);
            newFarmer.setName("Kisan (Registered)");
            newFarmer.setPhone(phone);
            String phoneSuffix = phone.length() >= 6 ? phone.substring(phone.length() - 6) : phone;
            String last4 = phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone;
            newFarmer.setKisanId("MP-FARM-" + phoneSuffix);
            newFarmer.setDistrict("Sehore");
            newFarmer.setVillage("Mandi Gram");
            newFarmer.setLandSizeAcres(new BigDecimal("4.50"));
            newFarmer.setMaskedAadhar("XXXX-XXXX-" + last4);
            newFarmer.setBankAccountLast4(last4);
            newFarmer.setIfscCode("SBIN0001234");
            return farmerRepository.save(newFarmer);
        });

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        UserDto userDto = toUserDto(user, farmer);
        return new AuthResponse(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpirationMs(), userDto);
    }

    public AuthResponse adminLogin(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Invalid username or password"));

        // Support standard hashed password verification, with fallback for the official demo admin
        boolean matches = false;
        if (user.getPasswordHash() != null) {
            matches = passwordEncoder.matches(password, user.getPasswordHash());
        } else if ("MP-AGRI-ADMIN-701".equals(username) && "Admin@MPMandi2026".equals(password)) {
            matches = true;
        }

        if (!matches) {
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid username or password");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        UserDto userDto = new UserDto(user.getId(), user.getUsername(), user.getPhone(), user.getRole().name(), user.getMandiId());
        userDto.setName("Administrator (" + user.getMandiId() + ")");
        userDto.setDistrict("Sehore");

        return new AuthResponse(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpirationMs(), userDto);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("INVALID_TOKEN", "Invalid or expired refresh token");
        }

        String username = jwtTokenProvider.getUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        Optional<Farmer> farmerOpt = farmerRepository.findByUserId(user.getId());
        UserDto userDto = farmerOpt.map(farmer -> toUserDto(user, farmer))
                .orElseGet(() -> new UserDto(user.getId(), user.getUsername(), user.getPhone(), user.getRole().name(), user.getMandiId()));

        return new AuthResponse(newAccessToken, newRefreshToken, jwtTokenProvider.getAccessTokenExpirationMs(), userDto);
    }

    private UserDto toUserDto(User user, Farmer farmer) {
        UserDto dto = new UserDto(user.getId(), user.getUsername(), user.getPhone(), user.getRole().name(), user.getMandiId());
        if (farmer != null) {
            dto.setName(farmer.getName());
            dto.setDistrict(farmer.getDistrict());
            dto.setVillage(farmer.getVillage());
            dto.setMaskedAadhar(farmer.getMaskedAadhar());
        }
        return dto;
    }
}
