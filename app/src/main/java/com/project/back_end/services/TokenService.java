package com.project.back_end.services;

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Creates and checks the JWTs that protect the REST endpoints and dashboards.
 *
 * The token subject is the user's email. The admin account has no email column, so for admins
 * the username is used as the subject instead; {@link #validateToken} looks the subject up in the
 * right table for the requested role.
 */
@Component
public class TokenService {

    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    // Secret comes from application.properties (jwt.secret); it must be at least 32 characters.
    @Value("${jwt.secret}")
    private String jwtSecret;

    public TokenService(AdminRepository adminRepository,
                        DoctorRepository doctorRepository,
                        PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    /** Key used to sign and verify tokens, derived from the configured secret. */
    public SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Builds a signed JWT for a user.
     *
     * @param email the user's email (for an admin, the username) - stored as the token subject
     * @return the compact JWT string, valid for 7 days
     */
    public String generateToken(String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + SEVEN_DAYS_MS))
                .signWith(getSigningKey())
                .compact();
    }

    /** Verifies the signature and expiry of the token and returns its subject (the email). */
    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Checks that the token is genuine, not expired, and belongs to an existing user of the given
     * role ("admin", "doctor" or "patient").
     *
     * @return true if the token is valid for that role, false for anything else
     */
    public boolean validateToken(String token, String user) {
        try {
            String subject = extractEmail(token);
            if (subject == null || user == null) {
                return false;
            }
            switch (user.toLowerCase()) {
                case "admin":
                    return adminRepository.findByUsername(subject) != null;
                case "doctor":
                    return doctorRepository.findByEmail(subject) != null;
                case "patient":
                    return patientRepository.findByEmail(subject) != null;
                default:
                    return false;
            }
        } catch (Exception e) {
            // bad signature, malformed or expired token
            return false;
        }
    }
}
