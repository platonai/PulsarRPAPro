package ai.platon.exotic.services.api

import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

/**
 *
 * */
class ServletInitializer : SpringBootServletInitializer() {

	override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
		return application
			.profiles("hsqldb")
			.initializers(ExoticContextInitializer(true))
			.sources(ExoticApplication::class.java)
	}
}
