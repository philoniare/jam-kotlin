import io.forge.jam.vrfs.BandersnatchWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RustLibraryTest {
    @Test
    fun testRingVrfCommitment() {
        val ringSize = 6
        val publicKeysHex = listOf(
            "0xaa2b95f7572875b0d0f186552ae745ba8222fc0b5bd456554bfe51c68938f8bc",
            "0xf16e5352840afb47e206b5c89f560f2611835855cf2e6ebad1acc9520a72591d",
            "0x5e465beb01dbafe160ce8216047f2155dd0569f058afd52dcea601025a8d161d",
            "0x48e5fcdce10e0b64ec4eebd0d9211c7bac2f27ce54bca6f7776ff6fee86ab3e3",
            "0x3d5e5a51aab2b048f8686ecd79712a80e3265a114cc73f14bdb2a59233fb66d0",
            "0x7f6190116d118d643a98878e294ccf62b509e214299931aad8ff9764181a4e33"
        )

        val publicKeys = publicKeysHex.map { hex ->
            hex.removePrefix("0x").chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }

        val bandersnatchWrapper = BandersnatchWrapper(ringSize)
        val commitmentBytes = bandersnatchWrapper.generateRingRoot(publicKeys, ringSize)
        assertNotNull(commitmentBytes, "Commitment should not be null")

        val commitmentHex = commitmentBytes.joinToString("") { "%02x".format(it) }

        val expectedGammaZ =
            "95f318fbd93287e8c3987874cded9b29adae70cf109c4321636fd9faeea003f8140710df8894ffbd6c84eaef4ff7cb58b17892749bb3cb3efe528eef951c688f7e83d3ede96f432de64f0b07e5c3cf0232f79ea4c221b7407f9a2348c0a0110692e630ae2b14e758ab0960e372172203f4c9a41777dadd529971d7ab9d23ab29fe0e9c85ec450505dde7f5ac038274cf"

        assertEquals(expectedGammaZ, commitmentHex, "Commitment does not match expected value")
    }

    @Test
    fun testRingVrfVerify() {
        val ringSize = 6
        val signatureHex =
            "b342bf8f6fa69c745daad2e99c92929b1da2b840f67e5e8015ac22dd1076343ea95c5bb4b69c197bfdc1b7d2f484fe455fb19bba7e8d17fcaf309ba5814bf54f3a74d75b408da8d3b99bf07f7cde373e4fd757061b1c99e0aac4847f1e393e892b566c14a7f8643a5d976ced0a18d12e32c660d59c66c271332138269cb0fe9c2462d5b3c1a6e9f5ed330ff0d70f64218010ff337b0b69b531f916c67ec564097cd842306df1b4b44534c95ff4efb73b17a14476057fdf8678683b251dc78b0b94712179345c794b6bd99aa54b564933651aee88c93b648e91a613c87bc3f445fff571452241e03e7d03151600a6ee259051a23086b408adec7c112dd94bd8123cf0bed88fddac46b7f891f34c29f13bf883771725aa234d398b13c39fd2a871894f1b1e2dbc7fffbc9c65c49d1e9fd5ee0da133bef363d4ebebe63de2b50328b5d7e020303499d55c07cae617091e33a1ee72ba1b65f940852e93e2905fdf577adcf62be9c74ebda9af59d3f11bece8996773f392a2b35693a45a5a042d88a3dc816b689fe596762d4ea7c6024da713304f56dc928be6e8048c651766952b6c40d0f48afc067ca7cbd77763a2d4f11e88e16033b3343f39bf519fe734db8a139d148ccead4331817d46cf469befa64ae153b5923869144dfa669da36171c20e1f757ed5231fa5a08827d83f7b478ddfb44c9bceb5c6c920b8761ff1e3edb03de48fb55884351f0ac5a7a1805b9b6c49c0529deb97e994deaf2dfd008825e8704cdc04b621f316b505fde26ab71b31af7becbc1154f9979e43e135d35720b93b367bedbe6c6182bb6ed99051f28a3ad6d348ba5b178e3ea0ec0bb4a03fe36604a9eeb609857f8334d3b4b34867361ed2ff9163acd9a27fa20303abe9fc29f2d6c921a8ee779f7f77d940b48bc4fce70a58eed83a206fb7db4c1c7ebe7658603495bb40f6a581dd9e235ba0583165b1569052f8fb4a3e604f2dd74ad84531c6b96723c867b06b6fdd1c4ba150cf9080aa6bbf44cc29041090973d56913b9dc755960371568ef1cf03f127fe8eca209db5d18829f5bfb5826f98833e3f42472b47fad995a9a8bb0e41a1df45ead20285a8"
        val signatureBytes = signatureHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val gammaZ =
            "96a4b479612d8d2770d6a4785fa2f44e0befca6f008b176de51e309e2ee796d2b596e315fcb044495b75c3cb5c7fd4cdae0959758cac93d4ab8789c6aec4ba8f683c6b103cf6888f70edfcb8dcbbc1d85a8fa3832e0cd4503c7a1796c8d0c3f792e630ae2b14e758ab0960e372172203f4c9a41777dadd529971d7ab9d23ab29fe0e9c85ec450505dde7f5ac038274cf"
        val gammaZBytes = gammaZ.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val entropy = "bb30a42c1e62f0afda5f0a4e8a562f7a13a24cea00ee81917b86b89e801314aa"
        val entropyBytes = entropy.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val attempt = 1L

        val bandersnatchWrapper = BandersnatchWrapper(ringSize)
        val result = bandersnatchWrapper.verifyRingProof(entropyBytes, attempt, signatureBytes, gammaZBytes)
        assert(!result.all { it == 0.toByte() })
    }
}
