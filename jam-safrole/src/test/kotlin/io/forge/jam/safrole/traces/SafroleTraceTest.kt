package io.forge.jam.safrole.traces

import kotlin.test.Test

/**
 * Tests for Safrole block authoring traces.
 * These traces test Safrole block authoring with VRF validation,
 * without work reports.
 */
class SafroleTraceTest : BaseTraceTest() {
    override val traceName = "safrole"

    @Test
    fun testSafroleStateChain() = runStateChainTests()

    @Test
    fun testSafroleEncoding() = runEncodingTests()

    @Test
    fun testSafroleBlockImport() = runTraceTests()

    @Test
    fun testSafroleDecoding() = runDecodingTests()
}
