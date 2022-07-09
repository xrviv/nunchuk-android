package com.nunchuk.android.signer.nfc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivityNfcSetupBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NfcSetupActivity : BaseNfcActivity<ActivityNfcSetupBinding>() {
    override fun initializeBinding(): ActivityNfcSetupBinding =
        ActivityNfcSetupBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLightStatusBar()
        initStartDestination()
    }

    private fun initStartDestination() {
        val navHostFragment = (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.nfc_setup_navigation)
        graph.startDestination = when (setUpAction) {
            SETUP_NFC -> R.id.setupChainCodeFragment
            CHANGE_CVC -> R.id.changeNfcCvcFragment
            RECOVER_NFC -> R.id.recoverNfcKeyGuideFragment
            else -> R.id.addNfcNameFragment
        }
        navHostFragment.navController.graph = graph
    }

    val setUpAction: Int
        get() = intent.getIntExtra(EXTRA_ACTION, SETUP_NFC)

    companion object {
        private const val EXTRA_ACTION = "EXTRA_ACTION"
        const val SETUP_NFC = 1
        const val ADD_KEY = 2
        const val CHANGE_CVC = 3
        const val RECOVER_NFC = 4

        fun navigate(activity: Activity, setUpAction: Int) {
            activity.startActivity(Intent(activity, NfcSetupActivity::class.java).apply {
                putExtra(EXTRA_ACTION, setUpAction)
            })
        }
    }
}