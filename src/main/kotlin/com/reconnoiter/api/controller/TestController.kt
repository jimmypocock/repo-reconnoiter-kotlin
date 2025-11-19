package com.reconnoiter.api.controller

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication

import io.sentry.Sentry

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ConditionalOnWebApplication
@RestController
@RequestMapping("/test")
class TestController(
    @Value("\${sentry.dsn:NOT_SET}") private val sentryDsn: String
) {

    @GetMapping("/sentry")
    fun testSentry(): Map<String, String> {
        try {
            throw Exception("This is a test.")
        } catch (e: Exception) {
            Sentry.captureException(e)
        }
        return mapOf("message" to "Test exception sent to Sentry!")
    }

    @GetMapping("/sentry-config")
    fun checkSentryConfig(): Map<String, Any> {
        return mapOf(
            "dsn" to if (sentryDsn.isNotEmpty()) "${sentryDsn.take(30)}..." else "NOT_SET",
            "isEnabled" to Sentry.isEnabled(),
            "lastEventId" to (Sentry.getLastEventId()?.toString() ?: "none")
        )
    }
}
