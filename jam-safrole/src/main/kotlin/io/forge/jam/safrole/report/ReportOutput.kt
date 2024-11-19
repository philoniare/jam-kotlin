package io.forge.jam.safrole.report

import kotlinx.serialization.Serializable

@Serializable
data class ReportOutput(
    val ok: ReportOutputMarks? = null,
    @Serializable(with = ReportErrorCodeSerializer::class)
    val err: ReportErrorCode? = null
)
