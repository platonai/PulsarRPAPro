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
        assertEquals("PT12345H", d.toString())
        assertEquals("1 y, 149 ds and 9 hs", ExoticUtils.formatDuration(d.seconds))
//        println(d)
//        println(ExoticUtils.formatDuration(d.seconds))
    }

    @Test
    fun testNameGenerator() {
        val name = NameGenerator.gen()
        println(name)
    }
}
