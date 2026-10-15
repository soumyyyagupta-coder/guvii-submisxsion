package com.example.courserec.recommendation;

import com.example.courserec.auth.AuthService;
import com.example.courserec.auth.AuthenticatedUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final AuthService authService;

    public RecommendationController(RecommendationService recommendationService, AuthService authService) {
        this.recommendationService = recommendationService;
        this.authService = authService;
    }

    @GetMapping
    public List<RecommendationResponse> getRecommendations(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        AuthenticatedUser user = authService.requireUser(authorizationHeader);
        return recommendationService.getRecommendations(user.id());
    }
}
