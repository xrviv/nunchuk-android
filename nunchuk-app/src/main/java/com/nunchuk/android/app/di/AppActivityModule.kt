package com.nunchuk.android.app.di

import com.nunchuk.android.app.intro.GuestModeIntroActivity
import com.nunchuk.android.app.intro.GuestModeMessageIntroActivity
import com.nunchuk.android.app.intro.IntroActivity
import com.nunchuk.android.app.splash.GuestModeModule
import com.nunchuk.android.app.splash.SplashActivity
import com.nunchuk.android.app.splash.SplashModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface AppActivityModule {

    @ContributesAndroidInjector(modules = [SplashModule::class])
    fun splashActivity(): SplashActivity

    @ContributesAndroidInjector
    fun introActivity(): IntroActivity

    @ContributesAndroidInjector(modules = [GuestModeModule::class])
    fun guestModeIntroActivity(): GuestModeIntroActivity

    @ContributesAndroidInjector
    fun guestModeMessageIntroActivity(): GuestModeMessageIntroActivity

}
