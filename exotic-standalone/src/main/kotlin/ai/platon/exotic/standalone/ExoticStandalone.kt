package ai.platon.exotic.standalone

import ai.platon.exotic.ExoticServerApplication
import ai.platon.exotic.services.ExoticApplication
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(ExoticApplication::class, ExoticServerApplication::class)
class ExoticStandalone

fun main(args: Array<String>) {
    SpringApplicationBuilder(ExoticStandalone::class.java)
        .profiles("h2")
        .initializers(ScentContextInitializer())
        .registerShutdownHook(true)
        .run(*args)
}
