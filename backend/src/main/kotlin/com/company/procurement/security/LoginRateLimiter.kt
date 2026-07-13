package com.company.procurement.security

import com.company.procurement.exception.RateLimitExceededException
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple in-memory sliding-window rate limiter for the login endpoint, protecting
 * against brute-force credential guessing. Deliberately dependency-free (no Redis/
 * bucket4j) since a single-instance deployment is this project's target scale;
 * the window is keyed per client IP so one attacker can't lock out other users.
 *
 * Not a substitute for a WAF/API-gateway rate limiter in a real production
 * deployment behind a load balancer, but meaningful defense-in-depth for a
 * directly-exposed instance.
 */
@Component
class LoginRateLimiter {

    companion object {
        private const val MAX_ATTEMPTS = 10
        private const val WINDOW_SECONDS = 60L
    }

    private val attemptsByKey = ConcurrentHashMap<String, MutableList<Instant>>()

    fun checkAllowed(key: String) {
        val now = Instant.now()
        val windowStart = now.minusSeconds(WINDOW_SECONDS)

        val attempts = attemptsByKey.computeIfAbsent(key) { mutableListOf() }

        synchronized(attempts) {
            attempts.removeAll { it.isBefore(windowStart) }
            if (attempts.size >= MAX_ATTEMPTS) {
                throw RateLimitExceededException(
                    "Too many login attempts. Please wait a minute before trying again."
                )
            }
            attempts.add(now)
        }
    }

    /** Called on a successful login to stop a legitimate user's later mistypes from compounding with earlier failures. */
    fun reset(key: String) {
        attemptsByKey.remove(key)
    }
}
