package com.chat.chat.Security;

import jakarta.annotation.PostConstruct;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);
    private static final String JWT_SECRET_PROP = "${app.jwt.secret:}";
    private static final String JWT_EXPIRATION_PROP = "${app.jwt.expiration:86400000}";
    private static final int HS256_MIN_KEY_BYTES = 32;

    @Value(JWT_SECRET_PROP)
    private String secretKey;

    @Value(JWT_EXPIRATION_PROP) // 1 day in milliseconds
    private long jwtExpiration;

    private Key signingKey;

    @PostConstruct
    void initSigningKey() {
        this.signingKey = buildSigningKey(secretKey);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        if (signingKey == null) {
            signingKey = buildSigningKey(secretKey);
        }
        return signingKey;
    }

    private Key buildSigningKey(String configuredSecret) {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            return keyFromBase64(configuredSecret.trim(), "app.jwt.secret");
        }

        byte[] randomBytes = new byte[HS256_MIN_KEY_BYTES];
        new SecureRandom().nextBytes(randomBytes);
        LOGGER.warn("app.jwt.secret is not configured. Using an ephemeral key for this process only.");
        return Keys.hmacShaKeyFor(randomBytes);
    }

    private Key keyFromBase64(String base64Secret, String sourceName) {
        try {
            byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(base64Secret);
            if (keyBytes.length < HS256_MIN_KEY_BYTES) {
                throw new IllegalStateException(sourceName + " must decode to at least 32 bytes for HS256");
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(sourceName + " must be valid Base64", ex);
        }
    }
}
