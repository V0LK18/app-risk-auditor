package com.example.appriskauditor.domain.model

data class RiskResult(
    val score: Int,
    val band: RiskBand,
    val reasons: List<Reason>
)
