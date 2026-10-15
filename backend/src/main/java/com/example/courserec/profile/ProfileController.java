package com.example.courserec.profile;

import com.example.courserec.auth.AuthService;
import com.example.courserec.auth.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final AuthService authService;
    private final ProfileService profileService;

    public ProfileController(AuthService authService, ProfileService profileService) {
        this.authService = authService;
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ProfileDto.ProfileResponse> getProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        AuthenticatedUser user = authService.requireUser(authHeader);
        ProfileDto.ProfileResponse profile = profileService.getProfile(user.id());
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<ProfileDto.ProfileResponse> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ProfileDto.ProfileUpdateRequest request
    ) {
        AuthenticatedUser user = authService.requireUser(authHeader);
        ProfileDto.ProfileResponse updated = profileService.updateProfile(user.id(), request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        AuthenticatedUser user = authService.requireUser(authHeader);
        profileService.deleteAccount(user.id());
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }
}
