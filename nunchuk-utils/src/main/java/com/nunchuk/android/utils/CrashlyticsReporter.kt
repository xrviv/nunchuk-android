package com.nunchuk.android.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

object CrashlyticsReporter {

    fun recordException(t: Throwable) {
        Timber.e(t)
        FirebaseCrashlytics.getInstance().recordException(t)
    }

}