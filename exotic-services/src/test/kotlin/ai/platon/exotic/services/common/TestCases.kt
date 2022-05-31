package ai.platon.exotic.services.common

import ai.platon.exotic.services.api.entity.converters.ModelConverter
import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class TestCases {

    @Test
    fun testDateTime() {
        val dateTime = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault())
        println(dateTime)
        println(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        println(ZoneId.systemDefault())
        println(LocalDateTime.parse("2200-01-01T08:00"))

        println(Instant.now())
    }

    @Test
    fun testUnderline() {
        val converter = ModelConverter()
        println(converter.underlineStyle("aGoodName"))
        println(converter.underlineStyle("a_Good_Name"))
    }

    @Test
    fun testRange() {
        println(IntRange(100, 100))
        IntRange(100, 100).forEach {
            println(it)
        }
    }

    @Test
    fun testCron() {
        val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ)
        val parser = CronParser(cronDefinition)
        val quartzCron: Cron = parser.parse("0 0 * * * ?")
        quartzCron.validate()
        println(quartzCron.asString())
    }
}
