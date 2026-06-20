package com.example.util

import android.util.Log

/**
 * Mock Crashlytics implementation to demonstrate resilience and telemetry for hackathon.
 * In a real application, this would be `com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()`.
 */
object Crashlytics {
    fun recordException(e: Throwable) {
        Log.e("Crashlytics", "Recorded exception: ${e.message}", e)
    }
}
