package com.example.appriskauditor.domain.model

data class Reason(
    val text: String,
    val points: Int,
    val category: Category
)
