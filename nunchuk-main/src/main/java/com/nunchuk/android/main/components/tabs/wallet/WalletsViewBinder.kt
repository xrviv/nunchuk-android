package com.nunchuk.android.main.components.tabs.wallet

import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.get
import com.nunchuk.android.core.util.*
import com.nunchuk.android.main.R
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.widget.util.AbsViewBinder

internal class WalletsViewBinder(
    container: ViewGroup,
    wallets: List<Wallet>,
    val onItemClickListener: (String) -> Unit = {}
) : AbsViewBinder<Wallet>(container, wallets) {

    override val layoutId: Int = R.layout.item_wallet

    override fun bindItem(position: Int, model: Wallet) {
        container[position].apply {
            findViewById<TextView>(R.id.walletName).text = model.name
            findViewById<TextView>(R.id.btc).text = model.getBTCAmount()
            findViewById<TextView>(R.id.balance).text = model.getUSDAmount()
            findViewById<TextView>(R.id.config).text = model.getConfiguration()
            setOnClickListener { onItemClickListener(model.id) }
        }

    }
}