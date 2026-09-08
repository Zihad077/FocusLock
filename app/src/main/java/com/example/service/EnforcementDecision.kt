package com.example.service

sealed class EnforcementDecision {
    object Allow : EnforcementDecision()
    
    data class Block(
        val reason: BlockReason,
        val appName: String,
        val packageName: String,
        val usedMinutes: Int,
        val limitMinutes: Int
    ) : EnforcementDecision()
}

enum class BlockReason {
    ALWAYS_BLOCKED,
    DAILY_LIMIT_EXCEEDED,
    SESSION_LIMIT_EXCEEDED,
    SCHEDULE_ACTIVE,
    FOCUS_MODE_ACTIVE
}
