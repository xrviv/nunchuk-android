package com.nunchuk.android.wallet.di

import com.nunchuk.android.wallet.add.AddWalletActivity
import com.nunchuk.android.wallet.add.AddWalletModule
import com.nunchuk.android.wallet.assign.AssignSignerActivity
import com.nunchuk.android.wallet.assign.AssignSignerModule
import com.nunchuk.android.wallet.backup.BackupWalletActivity
import com.nunchuk.android.wallet.backup.BackupWalletModule
import com.nunchuk.android.wallet.config.WalletConfigActivity
import com.nunchuk.android.wallet.config.WalletConfigModule
import com.nunchuk.android.wallet.confirm.WalletConfirmActivity
import com.nunchuk.android.wallet.confirm.WalletConfirmModule
import com.nunchuk.android.wallet.details.WalletDetailsActivity
import com.nunchuk.android.wallet.details.WalletDetailsModule
import com.nunchuk.android.wallet.intro.WalletIntroActivity
import com.nunchuk.android.wallet.upload.UploadConfigurationActivity
import com.nunchuk.android.wallet.upload.UploadConfigurationModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface WalletActivityModule {

    @ContributesAndroidInjector
    fun walletIntroActivity(): WalletIntroActivity

    @ContributesAndroidInjector(modules = [AddWalletModule::class])
    fun addWalletActivity(): AddWalletActivity

    @ContributesAndroidInjector(modules = [AssignSignerModule::class])
    fun assignSignerActivity(): AssignSignerActivity

    @ContributesAndroidInjector(modules = [WalletConfirmModule::class])
    fun walletConfirmActivity(): WalletConfirmActivity

    @ContributesAndroidInjector(modules = [BackupWalletModule::class])
    fun backupWalletActivity(): BackupWalletActivity

    @ContributesAndroidInjector(modules = [UploadConfigurationModule::class])
    fun uploadConfigurationActivity(): UploadConfigurationActivity

    @ContributesAndroidInjector(modules = [WalletConfigModule::class])
    fun walletInfoActivity(): WalletConfigActivity

    @ContributesAndroidInjector(modules = [WalletDetailsModule::class])
    fun walletDetailsActivity(): WalletDetailsActivity

}