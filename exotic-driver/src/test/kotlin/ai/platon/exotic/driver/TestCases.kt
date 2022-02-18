package ai.platon.exotic.driver

import ai.platon.exotic.driver.common.NameGenerator
import org.junit.jupiter.api.Test
import java.time.Duration

class TestCases {
    @Test
    fun testNameGenerator() {
        val name = NameGenerator.gen()
        println(name)
    }
}
