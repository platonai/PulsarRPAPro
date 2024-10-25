package ai.platon.exotic.standalone.api

import ai.platon.exotic.common.ExoticUtils
import ai.platon.exotic.server.ExoticServerApplication
import ai.platon.exotic.services.api.ExoticApplication
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ImportResource
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@Import(ExoticApplication::class, ExoticServerApplication::class)
@EnableScheduling
class StandaloneApplication

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
