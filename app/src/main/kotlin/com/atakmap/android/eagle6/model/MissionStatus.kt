package com.atakmap.android.eagle6.model

enum class MissionStatus {
    LAUNCHING, ON_TASK, RTH, LANDED;

    fun displayName(): String = when (this) {
        LAUNCHING -> "LAUNCHING"
        ON_TASK   -> "ON TASK"
        RTH       -> "RTH"
        LANDED    -> "LANDED"
    }
}
