package io.github.nexalloy.morphe

import io.github.nexalloy.AppVersion
import io.github.nexalloy.decodeCacheStringList
import io.github.nexalloy.encodeCacheStringList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
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

    @Test
    fun appVersionNormalizesMissingParts() {
        assertEquals(0, AppVersion("21.35").compareTo(AppVersion("21.35.0")))
        assertTrue(AppVersion("10.0") > AppVersion("9.999.999"))
        assertTrue(AppVersion("9999999999.0") > AppVersion("2147483647.999"))
    }

    @Test
    fun matchFirstOnlyAcceptsMethodStart() {
        val location = InstructionLocation.MatchFirst()
        assertTrue(location.indexIsValidForMatching(-1, 0))
        assertFalse(location.indexIsValidForMatching(-1, 1))
        assertThrows(IllegalArgumentException::class.java) {
            location.indexIsValidForMatching(0, 1)
        }
    }

    @Test
    fun matchAfterWithinRequiresPreviousMatch() {
        val location = InstructionLocation.MatchAfterWithin(2)
        assertTrue(location.indexIsValidForMatching(3, 4))
        assertTrue(location.indexIsValidForMatching(3, 6))
        assertFalse(location.indexIsValidForMatching(3, 7))
        assertFalse(location.indexIsValidForMatching(3, 3))
        assertFalse(location.indexIsValidForMatching(3, 2))
        assertThrows(IllegalArgumentException::class.java) {
            location.indexIsValidForMatching(-1, 0)
        }
    }

    @Test
    fun matchAfterRangeSupportsFirstFilter() {
        val location = InstructionLocation.MatchAfterRange(2, 4)
        assertFalse(location.indexIsValidForMatching(-1, 1))
        assertTrue(location.indexIsValidForMatching(-1, 2))
        assertTrue(location.indexIsValidForMatching(-1, 4))
        assertFalse(location.indexIsValidForMatching(-1, 5))
        assertTrue(location.indexIsValidForMatching(10, 13))
    }

    @Test
    fun smaliParsersSupportMultidimensionalArrays() {
        val method = methodCall("Lx;->m([[I[[Ljava/lang/String;)[[[D")
        assertEquals(listOf("[[I", "[[Ljava/lang/String;"), method.parameters)
        assertEquals("[[[D", method.returnType)

        val field = fieldAccess("Lx;->f:[[Ljava/lang/String;")
        assertEquals("[[Ljava/lang/String;", field.type)
    }

    @Test
    fun smaliParsersRejectVoidOutsideReturnType() {
        assertThrows(IllegalArgumentException::class.java) {
            methodCall("Lx;->m(V)V")
        }
        assertThrows(IllegalArgumentException::class.java) {
            fieldAccess("Lx;->f:V")
        }
    }

    @Test
    fun smaliParsersRejectEmptyObjectDescriptors() {
        assertThrows(IllegalArgumentException::class.java) {
            methodCall("Lx;->m(L;)V")
        }
        assertThrows(IllegalArgumentException::class.java) {
            methodCall("Lx;->m([L;)V")
        }
    }

    @Test
    fun cacheStringListsRoundTripLosslessly() {
        val cases = listOf(
            emptyList(),
            listOf(""),
            listOf("a|b", "c:d", "", "\u2603"),
            listOf("list-v1:5:hello")
        )

        cases.forEach { original ->
            assertEquals(original, decodeCacheStringList(encodeCacheStringList(original)))
        }
    }

    @Test
    fun cacheStringListDecoderRejectsMalformedValues() {
        assertNull(decodeCacheStringList("legacy|value"))
        assertNull(decodeCacheStringList("list-v1:nope:value"))
        assertNull(decodeCacheStringList("list-v1:10:short"))
        assertNull(decodeCacheStringList("list-v1:-1:"))
    }
}
