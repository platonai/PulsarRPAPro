package ai.platon.exotic.services.common.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

fun scentObjectMapper(): ObjectMapper = jsonMapper {
    addModule(JavaTimeModule())
    addModule(kotlinModule())
}.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

fun prettyScentObjectWritter() = scentObjectMapper().writerWithDefaultPrettyPrinter()
