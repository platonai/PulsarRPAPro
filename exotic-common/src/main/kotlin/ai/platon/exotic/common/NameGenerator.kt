package ai.platon.exotic.common

import ai.platon.pulsar.common.ResourceLoader
import org.apache.commons.lang3.RandomStringUtils
import kotlin.random.Random

object NameGenerator {
    val names = mutableListOf<String>()

    fun gen(): String {
        if (names.isEmpty()) {
            ResourceLoader.readAllLines("word_list.txt").distinct().toCollection(names)
        }

        if (names.isEmpty()) {
            return RandomStringUtils.randomAlphanumeric(5)
        }

        val index = Random.nextInt(names.size)
        return names[index].substringBefore(" ") + Random.nextInt(100)
    }
}
