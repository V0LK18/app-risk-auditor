package com.example.appriskauditor.domain.model

enum class Category(val displayName: String) {
    SPECIAL_ACCESS("Special Access"),
    SMS("SMS"),
    CONTACTS_CALLS("Contacts & Calls"),
    CAMERA_MIC("Camera & Microphone"),
    LOCATION("Location"),
    PHONE_IDENTITY("Phone Identity"),
    STORAGE_MEDIA("Storage & Media"),
    BACKGROUND("Background Execution"),
    NETWORK("Network"),
    OTHER("Other")
}
