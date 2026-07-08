package com.somosayni.asistente.infrastructure.config;

public interface JwtService {
    String generateToken(String userId, String email, String rol);
    String getUserIdFromToken(String token);
    boolean validateToken(String token);
}
