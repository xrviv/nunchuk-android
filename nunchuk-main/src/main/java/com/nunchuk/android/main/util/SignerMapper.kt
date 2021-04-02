package com.nunchuk.android.main.util

import com.nunchuk.android.model.SingleSigner
import java.util.regex.Pattern

object SignerMapper {

    fun toSigner(input: String): SignerInput {
        val pattern = Pattern.compile("^\\[([^/]+)/(.*)]xpub-([^/]+).*\$")
        val matcher = pattern.matcher(input)
        if (matcher.find()) {
            val fingerPrint = requireNotNull(matcher.group(1))
            val path = "m/${requireNotNull(matcher.group(2))}"
            val xpub = requireNotNull(matcher.group(3))
            return SignerInput(fingerPrint = fingerPrint, path = path, xpub = xpub)
        }
        throw InvalidSignerFormatException()
    }

    fun toSpec(signer: SingleSigner): String {
        val newPath = signer.derivationPath.replace("m/", "")
        return "[${signer.masterFingerprint}/$newPath]xpub-${signer.xpub}"
    }
}

data class SignerInput(val fingerPrint: String, val path: String, val xpub: String)

class InvalidSignerFormatException : Exception()