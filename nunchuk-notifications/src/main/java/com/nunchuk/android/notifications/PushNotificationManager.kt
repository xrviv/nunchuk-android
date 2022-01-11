package com.nunchuk.android.notifications

import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.provider.AppInfoProvider
import com.nunchuk.android.core.provider.LocaleProvider
import com.nunchuk.android.core.provider.StringProvider
import org.matrix.android.sdk.api.session.pushers.PushersService
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

interface PushNotificationManager {

    suspend fun testPush(pushKey: String)

    fun enqueueRegisterPusherWithFcmKey(pushKey: String): UUID?

    suspend fun registerPusherWithFcmKey(pushKey: String)

}

internal class PushNotificationManagerImpl @Inject constructor(
    private val localeProvider: LocaleProvider,
    private val stringProvider: StringProvider,
    private val appInfoProvider: AppInfoProvider
) : PushNotificationManager {

    override suspend fun testPush(pushKey: String) {
        SessionHolder.activeSession?.testPush(
            stringProvider.getString(R.string.push_http_url),
            stringProvider.getString(R.string.push_app_id),
            pushKey,
            TEST_EVENT_ID
        )
    }

    override fun enqueueRegisterPusherWithFcmKey(pushKey: String): UUID? {
        return SessionHolder.activeSession?.enqueueAddHttpPusher(createHttpPusher(pushKey))
    }

    override suspend fun registerPusherWithFcmKey(pushKey: String) {
        SessionHolder.activeSession?.addHttpPusher(createHttpPusher(pushKey))
    }

    private fun createHttpPusher(pushKey: String) = PushersService.HttpPusher(
        pushKey,
        stringProvider.getString(R.string.push_app_id),
        profileTag = "mobile" + "_" + abs(SessionHolder.activeSession?.myUserId.orEmpty().hashCode()),
        localeProvider.current().language,
        appInfoProvider.getAppName(),
        SessionHolder.activeSession?.sessionParams?.deviceId ?: "MOBILE",
        stringProvider.getString(R.string.push_http_url),
        append = false,
        withEventIdOnly = true
    )

    suspend fun unregisterPusher(pushKey: String) {
        SessionHolder.activeSession?.removeHttpPusher(pushKey, stringProvider.getString(R.string.push_app_id))
    }

    companion object {
        private const val TEST_EVENT_ID = "TEST_EVENT_ID"
    }
}
