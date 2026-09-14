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
import java.util.UUID;

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
     * - If unauthenticated/no context:
     *     * Throws AccessDeniedException (Fail-Closed).
     */
    public String resolveAndAuthorizeDistrict(String requestedDistrict) {
        Optional<SecurityUserContext> contextOpt = getCurrentUserContext();
        if (contextOpt.isEmpty()) {
            log.warn("Access Denied: Unauthenticated caller attempted to resolve district [{}]", requestedDistrict);
            throw new AccessDeniedException("Access denied: Authentication required");
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

        if (context.isAdmin()) {
            return (requestedDistrict != null && !requestedDistrict.isBlank()) ? requestedDistrict.trim() : null;
        }

        if (context.isMandiScoped()) {
            String assignedDistrict = context.getDistrict();
            if (assignedDistrict == null || assignedDistrict.isBlank()) {
                throw new AccessDeniedException("Access denied: Mandi user has no assigned district");
            }
            if (requestedDistrict != null && !requestedDistrict.isBlank()) {
                String cleanRequested = requestedDistrict.trim();
                if (!assignedDistrict.equalsIgnoreCase(cleanRequested)) {
                    throw new AccessDeniedException("Access denied: You are only authorized to access district: " + assignedDistrict);
                }
            }
            return assignedDistrict;
        }

        // For ROLE_FARMER or other authenticated roles
        return (requestedDistrict != null && !requestedDistrict.isBlank()) ? requestedDistrict.trim() : context.getDistrict();
    }

    /**
     * Verifies whether the current user is authorized to access resources associated with the specified district.
     * Throws AccessDeniedException if unauthorized or unauthenticated.
     */
    public void verifyDistrictAccess(String resourceDistrict) {
        if (resourceDistrict == null || resourceDistrict.isBlank()) {
            throw new AccessDeniedException("Access denied: District is required");
        }

        Optional<SecurityUserContext> contextOpt = getCurrentUserContext();
        if (contextOpt.isEmpty()) {
            log.warn("Access Denied: Unauthenticated access attempt to district [{}]", resourceDistrict);
            throw new AccessDeniedException("Access denied: Authentication required");
        }

        SecurityUserContext context = contextOpt.get();
        if (context.isAdmin()) {
            return;
        }

        if (context.isDistrictOfficer()) {
            String assignedDistrict = context.getDistrict();
            if (assignedDistrict == null || assignedDistrict.isBlank()) {
                throw new AccessDeniedException("Access denied: District Officer has no assigned district");
            }
            if (!assignedDistrict.equalsIgnoreCase(resourceDistrict.trim())) {
                log.warn("Access Denied: District Officer {} attempted to access resource in district [{}] but is assigned to [{}]",
                        context.getUsername(), resourceDistrict, assignedDistrict);
                throw new AccessDeniedException("Access denied: You are only authorized to access resources in district: " + assignedDistrict);
            }
            return;
        }

        if (context.isMandiScoped()) {
            String assignedDistrict = context.getDistrict();
            if (assignedDistrict == null || !assignedDistrict.equalsIgnoreCase(resourceDistrict.trim())) {
                log.warn("Access Denied: Mandi scoped user {} attempted to access resource in district [{}] but is assigned to [{}]",
                        context.getUsername(), resourceDistrict, assignedDistrict);
                throw new AccessDeniedException("Access denied: You are only authorized to access resources in district: " + assignedDistrict);
            }
            return;
        }

        if (context.getRole() == Role.ROLE_FARMER) {
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role: " + context.getRole());
    }

    /**
     * Verifies whether the current user is authorized to access a mandi.
     */
    public void verifyMandiAccess(String mandiId) {
        if (mandiId == null || mandiId.isBlank()) {
            throw new AccessDeniedException("Access denied: Mandi ID is required");
        }

        Optional<SecurityUserContext> contextOpt = getCurrentUserContext();
        if (contextOpt.isEmpty()) {
            log.warn("Access Denied: Unauthenticated access attempt to mandi [{}]", mandiId);
            throw new AccessDeniedException("Access denied: Authentication required");
        }

        SecurityUserContext context = contextOpt.get();
        if (context.isAdmin()) {
            return;
        }

        if (context.getRole() == Role.ROLE_FARMER) {
            return;
        }

        if (context.isMandiScoped()) {
            String userMandiId = context.getMandiId();
            if (userMandiId == null || userMandiId.isBlank()) {
                throw new AccessDeniedException("Access denied: Mandi operator/manager has no assigned mandi");
            }
            if (!userMandiId.equalsIgnoreCase(mandiId.trim())) {
                log.warn("Access Denied: Mandi scoped user {} assigned to [{}] attempted to access mandi [{}]",
                        context.getUsername(), userMandiId, mandiId);
                throw new AccessDeniedException("Access denied: You are only authorized to access mandi: " + userMandiId);
            }
            return;
        }

        if (context.isDistrictOfficer()) {
            String assignedDistrict = context.getDistrict();
            if (assignedDistrict == null || assignedDistrict.isBlank()) {
                throw new AccessDeniedException("Access denied: District Officer has no assigned district");
            }
            Optional<Mandi> mandiOpt = mandiRepository.findById(mandiId.trim());
            if (mandiOpt.isEmpty() || !assignedDistrict.equalsIgnoreCase(mandiOpt.get().getDistrict())) {
                throw new AccessDeniedException("Access denied: You are only authorized to access mandis in district: " + assignedDistrict);
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role: " + context.getRole());
    }

    /**
     * Verifies whether the current user or explicitly passed username is authorized to cancel a booking.
     */
    public void verifyBookingCancellation(com.kisansarthi.booking.Booking booking, String username) {
        if (booking == null) {
            throw new AccessDeniedException("Access denied: Booking is required");
        }

        Optional<SecurityUserContext> contextOpt = getCurrentUserContext();
        User user = null;

        if (contextOpt.isPresent()) {
            user = userRepository.findByUsername(contextOpt.get().getUsername()).orElse(null);
        } else if (username != null && !username.isBlank()) {
            user = userRepository.findByUsername(username.trim()).orElse(null);
        }

        if (user == null) {
            log.warn("Access Denied: Unauthenticated cancellation attempt for booking {}", booking.getId());
            throw new AccessDeniedException("Access denied: Authentication required for booking cancellation");
        }

        Role role = user.getRole();
        if (role == Role.ROLE_ADMIN) {
            return;
        }

        if (role == Role.ROLE_FARMER) {
            Farmer bookingFarmer = booking.getFarmer();
            if (bookingFarmer == null) {
                throw new AccessDeniedException("Access denied: Booking has no associated farmer");
            }
            Optional<Farmer> callerFarmer = farmerRepository.findByUserId(user.getId());
            if (callerFarmer.isEmpty()) {
                throw new AccessDeniedException("Access denied: Farmer profile not found for user: " + user.getUsername());
            }
            if (!callerFarmer.get().getId().equals(bookingFarmer.getId())) {
                throw new AccessDeniedException("Access denied: You are only authorized to cancel your own bookings");
            }
            return;
        }

        if (role == Role.ROLE_MANDI_OPERATOR || role == Role.ROLE_MANDI_MANAGER) {
            String userMandiId = user.getMandiId();
            if (userMandiId == null || userMandiId.isBlank()) {
                throw new AccessDeniedException("Access denied: Mandi operator/manager has no assigned mandi");
            }
            if (booking.getMandi() == null || !userMandiId.equalsIgnoreCase(booking.getMandi().getId())) {
                throw new AccessDeniedException("Access denied: You are only authorized to cancel bookings for mandi: " + userMandiId);
            }
            return;
        }

        if (role == Role.ROLE_DISTRICT_OFFICER) {
            if (booking.getMandi() == null) {
                throw new AccessDeniedException("Access denied: Booking has no associated mandi");
            }
            verifyDistrictAccess(booking.getMandi().getDistrict());
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role: " + role);
    }

    /**
     * Verifies whether the current user is authorized to read or mutate a booking.
     */
    public void verifyBookingAccess(com.kisansarthi.booking.Booking booking) {
        if (booking == null) {
            throw new AccessDeniedException("Access denied: Booking is required");
        }

        Optional<SecurityUserContext> contextOpt = getCurrentUserContext();
        if (contextOpt.isEmpty()) {
            log.warn("Access Denied: Unauthenticated access attempt for booking {}", booking.getId());
            throw new AccessDeniedException("Access denied: Authentication required");
        }

        SecurityUserContext context = contextOpt.get();
        if (context.isAdmin()) {
            return;
        }

        if (context.getRole() == Role.ROLE_FARMER) {
            User user = userRepository.findByUsername(context.getUsername()).orElse(null);
            if (user == null) {
                throw new AccessDeniedException("Access denied: User not found: " + context.getUsername());
            }
            Farmer bookingFarmer = booking.getFarmer();
            if (bookingFarmer == null) {
                throw new AccessDeniedException("Access denied: Booking has no associated farmer");
            }
            Optional<Farmer> callerFarmer = farmerRepository.findByUserId(user.getId());
            if (callerFarmer.isEmpty()) {
                throw new AccessDeniedException("Access denied: Farmer profile not found for user: " + context.getUsername());
            }
            if (!callerFarmer.get().getId().equals(bookingFarmer.getId())) {
                throw new AccessDeniedException("Access denied: You are only authorized to access your own bookings");
            }
            return;
        }

        if (context.isMandiScoped()) {
            String userMandiId = context.getMandiId();
            if (userMandiId == null || userMandiId.isBlank()) {
                throw new AccessDeniedException("Access denied: Mandi operator/manager has no assigned mandi");
            }
            if (booking.getMandi() == null || !userMandiId.equalsIgnoreCase(booking.getMandi().getId())) {
                throw new AccessDeniedException("Access denied: You are only authorized to access bookings for mandi: " + userMandiId);
            }
            return;
        }

        if (context.isDistrictOfficer()) {
            String assignedDistrict = context.getDistrict();
            if (assignedDistrict == null || assignedDistrict.isBlank()) {
                throw new AccessDeniedException("Access denied: District Officer has no assigned district");
            }
            if (booking.getMandi() == null || !assignedDistrict.equalsIgnoreCase(booking.getMandi().getDistrict())) {
                throw new AccessDeniedException("Access denied: You are only authorized to access bookings in district: " + assignedDistrict);
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unauthorized role: " + context.getRole());
    }
}
