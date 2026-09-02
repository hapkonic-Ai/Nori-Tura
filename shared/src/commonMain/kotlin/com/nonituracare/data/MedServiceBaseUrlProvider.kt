package com.nonituracare.data

/**
 * Base URL for the MedService semantic-autocomplete API.
 *
 * This is intentionally separate from the main backend URL because the
 * medical-terminology service is hosted independently and is currently
 * unauthenticated.
 */
expect fun getMedServiceBaseUrl(): String
