package io.forge.jam.pvm

import io.forge.jam.core.encoding.TestFileLoader
import io.forge.jam.pvm.engine.*
import io.forge.jam.pvm.program.ProgramBlob
import io.forge.jam.pvm.program.ProgramCounter
import io.forge.jam.pvm.program.ProgramParts
import io.forge.jam.pvm.program.Reg
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PvmTest {
    @Test
    fun runTest() {
        val folderName = "pvm"
        val testCases = TestFileLoader.getTestFilenamesFromResources(folderName)

        for (testCase in testCases) {
            val inputCase = TestFileLoader.loadJsonData<PvmCase>(
                "$folderName/$testCase",
            )

            val config = Config.new()
            val engine = Engine.new(config).getOrThrow()

            val parts = ProgramParts.fromRaw(inputCase.program).getOrThrow()
            val blob = ProgramBlob.fromParts(parts).getOrThrow()

            val moduleConfig = ModuleConfig.new()
            moduleConfig.setStrict(true)
            moduleConfig.setGasMetering(GasMeteringKind.Sync)
            moduleConfig.setStepTracing(true)

            val module = Module.fromBlob(engine, moduleConfig, blob).getOrThrow()
            val instance = module.instantiate().getOrThrow()

            // Init instance state
            instance.setGas(inputCase.initialGas)
            val programCounter = ProgramCounter(inputCase.initialPc)
            instance.setNextProgramCounter(programCounter)

            inputCase.initialRegs.forEachIndexed { index, value ->
                instance.setReg(Reg.fromRaw(index)!!, value.toULong())
            }

            var finalPc = inputCase.initialPc
            val expectedStatus = run {
                while (true) {
                    when (val result = instance.run().getOrThrow()) {
                        InterruptKind.Finished -> return@run "halt"
                        InterruptKind.Trap -> return@run "trap"
                        InterruptKind.NotEnoughGas -> return@run "out-of-gas"
                        InterruptKind.Step -> {
                            finalPc = instance.programCounter()!!.value
                            continue
                        }

                        is InterruptKind.Ecalli -> TODO()
                        is InterruptKind.Segfault -> TODO()
                    }
                }
            }
            if (expectedStatus != "halt") {
                finalPc = instance.programCounter()!!.value
            }

            // Validate output state
            assertEquals(inputCase.expectedPc, finalPc, "Program counter mismatch.")
            // Validate reg values
//            inputCase.expectedRegs.forEachIndexed { index, value ->
//                assertEquals(value, instance.reg(index).getOrThrow(), "Register $index mismatch.")
//            }
//
//            // Validate memory update
//            inputCase.initialPageMap.forEachIndexed { index, page ->
//                assertEquals(page, instance.pageMap(index).getOrThrow(), "Page map $index mismatch.")
//            }
//            assertEquals(inputCase.expectedGas, instance.gas(), "Gas mismatch.")
        }
    }
}
