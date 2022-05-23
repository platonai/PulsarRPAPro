package ai.platon.exotic.standalone

import ai.platon.exotic.standalone.common.VerboseHarvester
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import ai.platon.scent.ql.h2.ScentSQLSession
import ai.platon.scent.ql.h2.context.ScentSQLContexts
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import org.springframework.boot.builder.SpringApplicationBuilder

class ExoticExecutor(val argv: Array<String>) {
    val session = ScentSQLContexts.createSession()
    var harvest = false
    var scrape = false
    var configuredUrl = ""

    var scrapeFields = mutableListOf<String>()
    var headless = false

    var sql = ""

    constructor(args: String): this(args.split(" ").toTypedArray())

    fun scrape(): List<Map<String, String?>> {
        val (portalUrl, args) = UrlUtils.splitUrlArgs(configuredUrl)
        if (!UrlUtils.isValidUrl(portalUrl)) {
            System.err.println("The portal url is invalid")
            return listOf()
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        val options = session.options(args)

        val hasOutLinkSelector = listOf("outLinkSelector", "outLinkPattern").any { !options.isDefault(it) }
        val results = if (hasOutLinkSelector) {
            session.scrapeOutPages(portalUrl, args, scrapeFields)
        } else {
            listOf(session.scrape(portalUrl, args, scrapeFields))
        }

        if (results.size == 1) {
            println(gson.toJson(results[0]))
        } else {
            println(gson.toJson(results))
        }

        return results
    }

    fun harvest() {
        val (portalUrl, args) = UrlUtils.splitUrlArgs(configuredUrl)
        if (!UrlUtils.isValidUrl(portalUrl)) {
            System.err.println("The portal url is invalid")
            return
        }

        runBlocking {
            VerboseHarvester().harvest(portalUrl, args)
        }
    }

    fun executeSQL() {
        val context = ScentSQLContexts.create()
        val rs = context.executeQuery(sql)
        println(ResultSetFormatter(rs, withHeader = true))
    }

    fun runServer() {
        SpringApplicationBuilder(StandaloneApplication::class.java)
            .profiles("h2")
            .initializers(ScentContextInitializer())
            .registerShutdownHook(true)
            .run(*argv)
    }

    fun parseCmdLine() {
        var i = 0
        while (i < argv.size - 1) {
            if (argv[i] == "harvest") {
                harvest = true
                configuredUrl = argv.drop(i + 1).joinToString(" ")
                break
            }
            if (argv[i] == "scrape") {
                scrape = true
                configuredUrl = argv.drop(i + 1).joinToString(" ")
                parseScrapeArgs()
                break
            }
            if (argv[i] == "sql") {
                sql = argv.drop(i + 1).joinToString(" ")
                break
            }
            if (argv[i] == "-headless") headless = true

            ++i
        }
    }

    private fun parseScrapeArgs() {
        val argv = configuredUrl.split(" ")
        var k = 0
        while (k < argv.size - 1) {
            if (argv[k] == "-field") scrapeFields.add(argv[++k])
            ++k
        }
    }

    fun dispatch() {
        when {
            harvest -> harvest()
            scrape -> scrape()
            sql.isNotBlank() -> executeSQL()
            else -> runServer()
        }
    }
}
