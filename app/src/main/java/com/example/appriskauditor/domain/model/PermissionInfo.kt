package com.example.appriskauditor.domain.model

data class PermissionInfo(
    val name: String,
    val granted: Boolean,
    val protectionLevel: ProtectionLevel,
    val category: Category,
    val description: String
)
