package ai.platon.exotic.driver

import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.pulsar.driver.utils.ResourceLoader
import ai.platon.pulsar.driver.utils.SQLTemplate
import java.nio.file.Files

fun main() {
    System.setProperty("scrape.server.port", "8182")
    System.setProperty("scrape.server.servlet.context-path", "/")

    val crawler = ExoticCrawler()

    val urls = ResourceLoader.readAllLines("sites/amazon/asin/urls.txt").shuffled().take(10)
    val sqlTemplate =
        """select
            |   dom_first_text(dom, '#productTitle') as `title`,
            |   dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as `listprice`,
            |   dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #price_inside_buybox') as `price`,
            |   array_join_to_string(dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a'), '|') as `categories`,
            |   dom_base_uri(dom) as `baseUri`
            |from
            |   load_and_select('{{url}} -i 1h', ':root')
            |""".trimMargin()

    crawler.use {
        val driver = it.driver

        val ids = mutableSetOf<String>()
        urls.forEach { url ->
            val sql = SQLTemplate(sqlTemplate).createSQL(url)
            val id = driver.submit(sql, asap = true)
            ids.add(id)
        }
        val path = Files.createTempFile("pulsar-", ".txt")
        Files.write(path, ids)
        println("Ids are written to $path")

        val gson = driver.createGson()

        // we may want to check the status of a scrape task with a specified id
        val status = driver.findById(ids.first())
        println(gson.toJson(status))

        // we may want to check our dashboard
        val dashboard = driver.dashboard()
        println(gson.toJson(dashboard))

        // we download all the scrape results
        val results = driver.download(pageSize = 10)
        println(gson.toJson(results))
    }
}
