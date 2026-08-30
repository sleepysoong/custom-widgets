package com.customwidgets.app.domain.model

/**
 * Sealed hierarchy for typed application errors.
 */
sealed class AppError(open val userMessage: String, override val cause: Throwable? = null) : Exception(userMessage, cause) {

    data class NetworkError(
        override val userMessage: String = "No internet connection. Please check your network.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)

    data class ApiError(
        val code: Int,
        override val userMessage: String = "AI service error ($code)",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)

    data class ParseError(
        val rawResponse: String,
        override val userMessage: String = "Failed to parse AI widget design. Response may have been truncated.",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)

    data class ValidationError(
        val field: String,
        override val userMessage: String
    ) : AppError(userMessage)

    data class WidgetRenderError(
        val widgetId: Long,
        override val userMessage: String = "Error rendering widget",
        override val cause: Throwable? = null
    ) : AppError(userMessage, cause)
}
