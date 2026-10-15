package com.example.courserec.auth;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
public class AuthService {

    private static final Duration SESSION_TTL = Duration.ofDays(7);
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthenticatedUser requireUser(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization token");
        }

        String prefix = "Bearer ";
        if (!authorizationHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization token");
        }

        String token = authorizationHeader.substring(prefix.length()).trim();
        if (token.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization token");
        }

        String tokenHash = hashToken(token);
        Timestamp now = Timestamp.from(Instant.now());

        String sql = """
                SELECT u.id, u.email, u.name, u.role
                FROM sessions s
                JOIN users u ON s.user_id = u.id
                WHERE s.token_hash = ? AND s.expires_at > ?
                """;

        List<AuthenticatedUser> users = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new AuthenticatedUser(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("name"),
                        rs.getString("role")
                ),
                tokenHash,
                now
        );

        if (users.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired session token");
        }

        return users.get(0);
    }

    public AuthenticatedUser requireAdmin(String authorizationHeader) {
        AuthenticatedUser user = requireUser(authorizationHeader);
        if (!"ADMIN".equalsIgnoreCase(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Administrator access required");
        }
        return user;
    }

    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        if (request.email() == null || request.email().trim().isEmpty() || !request.email().contains("@")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Valid email is required");
        }
        if (request.password() == null || request.password().length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        Integer existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE LOWER(email) = ?",
                Integer.class,
                normalizedEmail
        );

        if (existingCount != null && existingCount > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is already registered");
        }

        String name = request.name().trim();
        String passwordHash = passwordEncoder.encode(request.password());
        String role = "STUDENT";

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (name, email, password_hash, role, academic_background, proficiency_level, career_goal) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, name);
            ps.setString(2, normalizedEmail);
            ps.setString(3, passwordHash);
            ps.setString(4, role);
            ps.setString(5, "");
            ps.setString(6, "BEGINNER");
            ps.setString(7, "");
            return ps;
        }, keyHolder);

        long userId;
        if (keyHolder.getKey() != null) {
            userId = keyHolder.getKey().longValue();
        } else if (keyHolder.getKeys() != null && !keyHolder.getKeys().isEmpty()) {
            userId = ((Number) keyHolder.getKeys().values().iterator().next()).longValue();
        } else {
            Long fetchedId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE LOWER(email) = ?", Long.class, normalizedEmail);
            userId = fetchedId != null ? fetchedId : 0L;
        }

        String token = createSession(userId);
        return new AuthDto.AuthResponse(token, new AuthDto.UserSummary(userId, name, normalizedEmail, role));
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        if (request.email() == null || request.email().trim().isEmpty() ||
                request.password() == null || request.password().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        String sql = "SELECT id, name, email, password_hash, role FROM users WHERE LOWER(email) = ?";

        var userList = jdbcTemplate.query(sql, (rs, rowNum) -> new Object() {
            final long id = rs.getLong("id");
            final String name = rs.getString("name");
            final String email = rs.getString("email");
            final String passwordHash = rs.getString("password_hash");
            final String role = rs.getString("role");
        }, normalizedEmail);

        if (userList.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        var user = userList.get(0);
        if (!passwordEncoder.matches(request.password(), user.passwordHash)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = createSession(user.id);
        return new AuthDto.AuthResponse(token, new AuthDto.UserSummary(user.id, user.name, user.email, user.role));
    }

    public String createSession(long userId) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = HexFormat.of().formatHex(randomBytes);
        String tokenHash = hashToken(token);
        Timestamp expiresAt = Timestamp.from(Instant.now().plus(SESSION_TTL));

        jdbcTemplate.update(
                "INSERT INTO sessions (user_id, token_hash, expires_at) VALUES (?, ?, ?)",
                userId,
                tokenHash,
                expiresAt
        );

        return token;
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
