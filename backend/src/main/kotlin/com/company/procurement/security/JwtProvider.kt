package com.company.procurement.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val jwtSecret: String,
    @Value("\${jwt.expiration-ms}") private val jwtExpirationMs: Long
) {

    private val logger = LoggerFactory.getLogger(JwtProvider::class.java)

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    fun generateToken(userPrincipal: UserPrincipal): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationMs)

        return Jwts.builder()
            .subject(userPrincipal.username)
            .claim("userId", userPrincipal.id)
            .claim("role", userPrincipal.role)
            .claim("firstName", userPrincipal.firstName)
            .claim("lastName", userPrincipal.lastName)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(signingKey)
            .compact()
    }

    fun getEmailFromToken(token: String): String {
        return getClaims(token).subject
    }

    fun getRoleFromToken(token: String): String {
        return getClaims(token).get("role", String::class.java)
    }

    fun getUserIdFromToken(token: String): String {
        return getClaims(token).get("userId", String::class.java)
    }

    fun validateToken(token: String): Boolean {
        return try {
            getClaims(token)
            true
        } catch (ex: ExpiredJwtException) {
            logger.warn("JWT token expired: ${ex.message}")
            false
        } catch (ex: MalformedJwtException) {
            logger.warn("Malformed JWT token: ${ex.message}")
            false
        } catch (ex: SignatureException) {
            logger.warn("Invalid JWT signature: ${ex.message}")
            false
        } catch (ex: IllegalArgumentException) {
            logger.warn("JWT claims string is empty: ${ex.message}")
            false
        } catch (ex: Exception) {
            logger.warn("Invalid JWT token: ${ex.message}")
            false
        }
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
