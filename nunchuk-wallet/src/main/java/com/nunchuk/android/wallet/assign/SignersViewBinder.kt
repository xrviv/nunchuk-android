package com.nunchuk.android.wallet.assign

import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.wallet.R
import com.nunchuk.android.widget.util.AbsViewBinder
import java.util.*

internal class SignersViewBinder(
    container: ViewGroup,
    signers: List<SingleSigner>,
    private val selectedXpfs: List<String> = emptyList(),
    val onItemSelectedListener: (String, Boolean) -> Unit,
) : AbsViewBinder<SingleSigner>(container, signers) {

    override val layoutId: Int = R.layout.item_assign_signer

    override fun bindItem(position: Int, model: SingleSigner) {
        val itemView = container.getChildAt(position)
        val signerName = itemView.findViewById<TextView>(R.id.signerName)
        val avatar = itemView.findViewById<TextView>(R.id.avatar)
        avatar.text = model.name.shorten().toUpperCase(Locale.getDefault())
        val xfp = itemView.findViewById<TextView>(R.id.xpf)
        val checkBox = itemView.findViewById<CheckBox>(R.id.checkbox)
        signerName.text = model.name
        val xfpValue = "XFP: ${model.masterFingerprint}"
        xfp.text = xfpValue
        checkBox.isChecked = selectedXpfs.isNotEmpty() && selectedXpfs.contains(model.masterFingerprint)
        checkBox.setOnCheckedChangeListener { _, checked -> onItemSelectedListener(model.masterFingerprint, checked) }
    }
}