package io.github.nexalloy.morphe

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoreInvariantTest {
    @Suppress("DEPRECATION")
    @Test
    fun instanceOfUsesInstanceOfOpcode() {
        val filters = listOf(
            instanceOf("Ljava/lang/String;"),
            instanceOf({ "Ljava/lang/String;" }),
            instanceOf("Ljava/lang/String;", StringComparisonType.EQUALS),
            instanceOf({ "Ljava/lang/String;" }, StringComparisonType.EQUALS)
        )

        filters.forEach { filter ->
            assertEquals(Opcode.INSTANCE_OF, (filter as OpcodeFilter).opcode)
        }
    }

    @Test
    fun versionComparisonIsNumeric() {
        assertTrue(isVersionAtLeast("10.0.0", "9.33.00"))
        assertFalse(isVersionAtLeast("9.9.0", "9.19.0"))
        assertTrue(isVersionAtLeast("21.02.0", "21.02.000"))
        assertTrue(isVersionAtLeast("21.32", "21.32.0"))
        assertFalse(isVersionAtLeast("2026.3.9", "2026.04.0"))
    }
}
