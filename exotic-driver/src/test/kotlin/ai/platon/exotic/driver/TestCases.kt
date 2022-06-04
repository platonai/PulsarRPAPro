package ai.platon.exotic.driver

import ai.platon.exotic.driver.common.ExoticUtils
import ai.platon.exotic.driver.common.NameGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class TestCases {
    @Test
    fun testFormatDuration() {
        val d = Duration.parse("pt12345h")
        val longFormat = ExoticUtils.formatLongDuration(d.seconds)
        val shortFormat = ExoticUtils.formatShortDuration(d.seconds)
        val format = ExoticUtils.formatDuration(d.seconds)
        assertEquals("PT12345H", d.toString())
//        println(longFormat)
//        println(shortFormat)
        assertEquals("1 year, 149 days and 9 hours", longFormat)
        assertEquals("1 y, 149 ds and 9 hs", shortFormat)
        assertEquals(shortFormat, format)
    }

    @Test
    fun testNameGenerator() {
        val name = NameGenerator.gen()
        println(name)
    }
}
