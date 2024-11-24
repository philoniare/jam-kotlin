package io.forge.jam.safrole

import io.forge.jam.core.ByteArrayList
import io.forge.jam.core.Encodable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Psi(
    // Good/Valid Reports
    @SerialName("good")
    val good: ByteArrayList = ByteArrayList(),

    // Bad/Invalid Reports
    @SerialName("bad")
    val bad: ByteArrayList = ByteArrayList(),

    // Wonky/Unknown Reports
    @SerialName("wonky")
    val wonky: ByteArrayList = ByteArrayList(),

    // Offending validators
    @SerialName("offenders")
    val offenders: ByteArrayList = ByteArrayList(),
) : Encodable {

    fun copy(): Psi = Psi(
        good = ByteArrayList().apply { addAll(good) },
        bad = ByteArrayList().apply { addAll(bad) },
        wonky = ByteArrayList().apply { addAll(wonky) },
        offenders = ByteArrayList().apply { addAll(offenders) }
    )

    override fun encode(): ByteArray {
        return good.encode() + bad.encode() + wonky.encode() + offenders.encode()
    }
}
