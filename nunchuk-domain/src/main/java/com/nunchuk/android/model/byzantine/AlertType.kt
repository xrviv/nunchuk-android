package com.nunchuk.android.model.byzantine

import androidx.annotation.Keep

@Keep
enum class AlertType {
    NONE,
    GROUP_WALLET_PENDING,
    GROUP_WALLET_SETUP,
    GROUP_WALLET_INVITATION_DENIED,
    DRAFT_WALLET_KEY_ADDED,
    UPDATE_SERVER_KEY,
    UPDATE_SERVER_KEY_SUCCESS,
    CREATE_INHERITANCE_PLAN,
    UPDATE_INHERITANCE_PLAN,
    CANCEL_INHERITANCE_PLAN,
    UPDATE_INHERITANCE_PLAN_SUCCESS,
    KEY_RECOVERY_REQUEST,
    KEY_RECOVERY_SUCCESS,
    RECURRING_PAYMENT_REQUEST,
    RECURRING_PAYMENT_SUCCESS,
    HEALTH_CHECK_REQUEST,
    HEALTH_CHECK_PENDING,
    HEALTH_CHECK_COMPLETED
}

fun String?.toAlertType() = AlertType.values().find { it.name == this } ?: AlertType.NONE

fun AlertType.isInheritanceType() = this == AlertType.CREATE_INHERITANCE_PLAN ||
        this == AlertType.UPDATE_INHERITANCE_PLAN ||
        this == AlertType.CANCEL_INHERITANCE_PLAN