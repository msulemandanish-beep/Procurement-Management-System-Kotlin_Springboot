package com.company.procurement.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * Fails fast with a loud warning (rather than silently running insecurely) if the
 * application starts in the "prod" profile while still using the bundled default
 * JWT secret. Does not prevent startup — some environments intentionally run this
 * profile locally for testing — but makes the misconfiguration impossible to miss
 * in the logs.
 */
@Component
class StartupSecurityCheck(
    @Value("\${jwt.secret}") private val jwtSecret: String,
    private val environment: Environment
) {

    private val logger = LoggerFactory.getLogger(StartupSecurityCheck::class.java)

    companion object {
        private const val DEFAULT_SECRET = "ThisIsASecretKeyForProcurementManagementSystemThatMustBeAtLeast256BitsLong"
    }

    @EventListener(ApplicationReadyEvent::class)
    fun verifySecureConfiguration() {
        val isProd = environment.activeProfiles.contains("prod")

        if (jwtSecret == DEFAULT_SECRET) {
            if (isProd) {
                logger.error(
                    "SECURITY WARNING: the application is running with profile 'prod' but JWT_SECRET " +
                        "has not been overridden from its default value. Set the JWT_SECRET environment " +
                        "variable to a unique, random 256-bit+ value before exposing this instance publicly."
                )
            } else {
                logger.warn(
                    "Using the default JWT secret. This is acceptable for local development only — " +
                        "set the JWT_SECRET environment variable before deploying anywhere else."
                )
            }
        }
    }
}
