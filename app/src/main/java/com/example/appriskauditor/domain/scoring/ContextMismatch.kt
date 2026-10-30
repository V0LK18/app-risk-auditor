package com.example.appriskauditor.domain.scoring

import com.example.appriskauditor.domain.model.AppInfo
import com.example.appriskauditor.domain.model.Category
import com.example.appriskauditor.domain.model.Reason

object ContextMismatch {

    private val KEYWORDS = setOf(
        "torch",
        "flashlight",
        "calculator",
        "wallpaper",
        "keyboard",
        "qr",
        "scanner",
        "compass",
        "ringtone"
    )

    fun evaluate(app: AppInfo): Reason? {
        val nameLower = app.name.lowercase()
        val pkgLower = app.packageName.lowercase()

        val matchedKeyword = KEYWORDS.firstOrNull { nameLower.contains(it) || pkgLower.contains(it) }
            ?: return null

        val hasSmsRead = app.permissions.any { it.name.endsWith("READ_SMS") && it.granted }
        val hasContactsOrCallLog = app.permissions.any {
            (it.name.endsWith("READ_CONTACTS") || it.name.endsWith("READ_CALL_LOG")) && it.granted
        }
        val hasMic = app.permissions.any { it.name.endsWith("RECORD_AUDIO") && it.granted }
        val hasAccessibility = app.specialAccess.accessibilityEnabled || app.specialAccess.accessibilityDeclared

        if (!hasSmsRead && !hasContactsOrCallLog && !hasMic && !hasAccessibility) {
            return null
        }

        val isHigh = hasAccessibility || hasSmsRead
        val points = if (isHigh) RiskWeights.BONUS_CONTEXT_MISMATCH_HIGH else RiskWeights.BONUS_CONTEXT_MISMATCH_NORMAL

        val explanation = when {
            hasSmsRead -> "A $matchedKeyword app has no reason to read your text messages."
            hasAccessibility -> "A $matchedKeyword app has no reason to request or enable accessibility control."
            hasContactsOrCallLog -> "A $matchedKeyword app has no reason to access your contacts or call logs."
            hasMic -> "A $matchedKeyword app has no reason to record audio using the microphone."
            else -> "A $matchedKeyword app requests suspicious sensitive capabilities."
        }

        return Reason(
            text = explanation,
            points = points,
            category = Category.OTHER
        )
    }
}
