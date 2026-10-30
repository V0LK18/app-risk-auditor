package com.example.appriskauditor.domain.model

enum class RiskBand {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    companion object {
        fun fromScore(score: Int): RiskBand {
            return when {
                score >= 75 -> CRITICAL
                score >= 50 -> HIGH
                score >= 25 -> MEDIUM
                else -> LOW
            }
        }
    }
}
