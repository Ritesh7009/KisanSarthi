package com.kisansarthi.auth;

import com.kisansarthi.farmer.Farmer;
import com.kisansarthi.farmer.FarmerRepository;
import com.kisansarthi.mandi.Mandi;
import com.kisansarthi.mandi.MandiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityAuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuthorizationService.class);

    private final UserRepository userRepository;
    private final MandiRepository mandiRepository;
    private final FarmerRepository farmerRepository;

    public SecurityAuthorizationService(
            UserRepository userRepository,
            MandiRepository mandiRepository,
            FarmerRepository farmerRepository
    ) {
        this.userRepository = userRepository;
        this.mandiRepository = mandiRepository;
        this.farmerRepository = farmerRepository;
    }

    /**
     * Resolves the current authenticated user context, including authoritative district and mandiId.
     */
    public Optional<SecurityUserContext> getCurrentUserContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }

        String username = auth.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        String mandiId = user.getMandiId();
        String district = null;

        // Resolve district from Mandi if mandiId is present
        if (mandiId != null && !mandiId.isBlank()) {
            Optional<Mandi> mandiOpt = mandiRepository.findById(mandiId);
            if (mandiOpt.isPresent()) {
                district = mandiOpt.get().getDistrict();
            }
        }

        // If district is not resolved yet, check Farmer profile
        if (district == null && user.getId() != null) {
            Optional<Farmer> farmerOpt = farmerRepository.findByUserId(user.getId());
            if (farmerOpt.isPresent()) {
                district = farmerOpt.get().getDistrict();
            }
        }

        return Optional.of(new SecurityUserContext(username, user.getRole(), mandiId, district));
    }

    /**
     * Enforces district authorization rules:
     * - If ROLE_DISTRICT_OFFICER:
     *     * If client requests a different district than officer's assigned district -> throws AccessDeniedException (403).
     *     * If client requests own district or omits district -> returns officer's authoritative assigned district.
     * - If ROLE_ADMIN:
     *     * Can access all districts (if requestedDistrict is null/blank) or any specified district.
     * - If other authenticated roles:
     *     * Return requestedDistrict or user's own district if present.
     */
    public String resolveAndAuthorizeDistrict(String requestedDistrict) {
        Optional<SecurityUserContext> contextOpt = getCurrentUserContext();
        if (contextOpt.isEmpty()) {
            return (requestedDistrict != null && !requestedDistrict.isBlank()) ? requestedDistrict.trim() : null;
        }

        SecurityUserContext context = contextOpt.get();

        if (context.isDistrictOfficer()) {
            String assignedDistrict = context.getDistrict();
            if (assignedDistrict == null || assignedDistrict.isBlank()) {
                log.warn("District Officer {} has no assigned district in database", context.getUsername());
                throw new AccessDeniedException("District Officer has no assigned district jurisdiction configured");
            }

            if (requestedDistrict != null && !requestedDistrict.isBlank()) {
                String cleanRequested = requestedDistrict.trim();
                if (!assignedDistrict.equalsIgnoreCase(cleanRequested)) {
                    log.warn("Access Denied: District Officer {} assigned to [{}] attempted to access district [{}]",
                            context.getUsername(), assignedDistrict, cleanRequested);
                    throw new AccessDeniedException("Access denied: You are only authorized to access district: " + assignedDistrict);
                }
            }

            // Return authoritative assigned district
            return assignedDistrict;
        }

        // For ROLE_ADMIN and other roles, allow requested district if provided, or null (all districts)
        return (requestedDistrict != null && !requestedDistrict.isBlank()) ? requestedDistrict.trim() : null;
    }

    /**
     * Verifies whether the current user is authorized to access resources associated with the specified district.
     * Throws AccessDeniedException if unauthorized.
     */
    public void verifyDistrictAccess(String resourceDistrict) {
        if (resourceDistrict == null || resourceDistrict.isBlank()) {
            return;
        }

        Optional<SecurityUserContext> contextOpt = getCurrentUserContext();
        if (contextOpt.isEmpty()) {
            return;
        }

        SecurityUserContext context = contextOpt.get();
        if (context.isDistrictOfficer()) {
            String assignedDistrict = context.getDistrict();
            if (assignedDistrict == null || !assignedDistrict.equalsIgnoreCase(resourceDistrict.trim())) {
                log.warn("Access Denied: District Officer {} attempted to access resource in district [{}] but is assigned to [{}]",
                        context.getUsername(), resourceDistrict, assignedDistrict);
                throw new AccessDeniedException("Access denied: You are only authorized to access resources in district: " + assignedDistrict);
            }
        }
    }

    /**
     * Verifies whether the current user is authorized to access a mandi.
     */
    public void verifyMandiAccess(String mandiId) {
        if (mandiId == null || mandiId.isBlank()) {
            return;
        }

        Optional<SecurityUserContext> contextOpt = getCurrentUserContext();
        if (contextOpt.isEmpty()) {
            return;
        }

        SecurityUserContext context = contextOpt.get();
        if (context.isDistrictOfficer()) {
            Optional<Mandi> mandiOpt = mandiRepository.findById(mandiId);
            if (mandiOpt.isPresent()) {
                verifyDistrictAccess(mandiOpt.get().getDistrict());
            }
        }
    }
}
