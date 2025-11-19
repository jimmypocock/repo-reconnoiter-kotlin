package com.reconnoiter.api.security

import javax.crypto.SecretKey

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import java.util.*

@Component
class JwtUtil(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration:86400000}") private val expirationMs: Long // 24 hours default
) {

    //--------------------------------------
    // CONSTANTS
    //--------------------------------------

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    //--------------------------------------
    // PUBLIC INSTANCE METHODS
    //--------------------------------------

    fun generateToken(userId: Long, email: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("user_id", userId)
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun getEmailFromToken(token: String): String {
        val claims = getClaims(token)
        return claims.get("email", String::class.java)
    }

    fun getUserIdFromToken(token: String): Long {
        val claims = getClaims(token)
        return claims.get("user_id", Integer::class.java).toLong()
    }

    fun validateToken(token: String): Claims {
        return getClaims(token)
    }

    //--------------------------------------
    // PRIVATE METHODS
    //--------------------------------------

    private fun getClaims(token: String): Claims {
        // Let JJWT exceptions bubble up to caller (filter will handle them)
        // Expected exceptions: ExpiredJwtException, MalformedJwtException, SignatureException
        // Unexpected exceptions will bubble to Sentry
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
