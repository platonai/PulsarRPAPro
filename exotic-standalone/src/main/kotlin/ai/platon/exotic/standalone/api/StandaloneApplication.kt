package ai.platon.exotic.standalone.api

import ai.platon.exotic.common.ExoticUtils
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    scanBasePackages = [
        "ai.platon.scent.boot.autoconfigure",
        "ai.platon.scent.rest.api",
        "ai.platon.exotic.services.api",
        "ai.platon.exotic.standalone.api",
    ]
)
@EntityScan(
    "ai.platon.exotic.driver.crawl.entity",
    "ai.platon.exotic.services.entity"
)
@ImportResource("classpath:config/app/app-beans/app-context.xml")
@EnableMongoRepositories("ai.platon.scent.boot.autoconfigure.persist")
@EnableJpaRepositories("ai.platon.exotic.services.api.persist")
// should work but failed to import Applications
// @Import(ExoticApplication::class, ExoticServerApplication::class)
@EnableScheduling
class StandaloneApplication {
    
    @Bean
    fun javaTimeModule(): JavaTimeModule {
        return JavaTimeModule()
    }
}

fun main(argv: Array<String>) {
    ExoticUtils.prepareDatabaseOrFail()
    
    val addProfiles = mutableListOf("hsqldb")
    runApplication<StandaloneApplication> {
        setAdditionalProfiles(*addProfiles.toTypedArray())
        addInitializers(ScentContextInitializer())
        setRegisterShutdownHook(true)
        setLogStartupInfo(true)
    }
}
