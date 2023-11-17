package com.nunchuk.android.core.data.model.payment

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.model.payment.RecurringPayment

internal data class CreateRecurringPaymentRequest(
    @SerializedName("nonce") val nonce: String? = null,
    @SerializedName("body") val body: RecurringPaymentResponse? = null,
)

internal fun RecurringPayment.toRequest(nonce: String): CreateRecurringPaymentRequest {
    return CreateRecurringPaymentRequest(
        nonce = nonce,
        body = RecurringPaymentResponse(
            name = name,
            paymentType = paymentType.name,
            paymentPayload = PaymentPayload(
                value = amount,
                currency = currency,
                calculationMethod = calculationMethod?.name,
            ),
            destinationType = destinationType.name,
            destinationPayload = DestinationPayload(
                bsms = bsms,
                addresses = addresses,
            ),
            frequency = frequency.name,
            startDateMillis = startDate,
            endDateMillis = endDate.takeIf { it > 0L },
            allowCosigning = allowCosigning,
            transactionNote = note,
            feeRate = feeRate.name,
        )
    )
}