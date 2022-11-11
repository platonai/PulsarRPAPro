package ai.platon.exotic.standalone.starter

import ai.platon.exotic.standalone.api.StandaloneApplication
import ai.platon.exotic.standalone.common.VerboseHarvester
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.ql.ResultSets
import ai.platon.scent.boot.autoconfigure.ScentContextInitializer
import ai.platon.scent.dom.HarvestOptions
import ai.platon.scent.ql.h2.context.ScentSQLContexts
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import org.springframework.boot.builder.SpringApplicationBuilder
import java.sql.ResultSet
import kotlin.system.exitProcess

class ExoticExecutor(val argv: Array<String>) {
    private val session = ScentSQLContexts.createSession()

    /**
     * Do not print anything
     * */
    var mute = false

    var parsed = false
        private set
    var arrange = false
        private set
    var harvest = false
        private set
    var scrape = false
        private set
    var server = false
        private set
    var criticalHelp = false
        private set
    var help = false
        private set
    var configuredUrl = ""
        private set

    var scrapeFields = mutableListOf<String>()
    var headless = false
        private set

    var sql = ""
        private set

    var helpVerbose = false
        private set
    var helpArgs: Array<String>? = null
        private set

    var lastOutput: String? = null
        private set

    var lastHelpMessage: String? = null
        private set

    constructor(args: String) : this(args.split(" ").toTypedArray())

    fun execute() {
        parseCmdLine()

        if (criticalHelp) {
            System.err.println(MAIN_HELP)
            return
        }

        if (help) {
            help()
            return
        }

        if (headless) {
            BrowserSettings.headless()
        }

        when {
            arrange -> arrange()
            harvest -> harvest()
            scrape -> scrape()
            server -> runServer()
            sql.isNotBlank() -> executeSQL()
            else -> help()
        }
    }

    fun mute() {
        mute = true
    }

    private fun help() {
        val args = helpArgs
        if (args != null) {
            dispatchHelp(args)
        } else {
            lastHelpMessage = MAIN_HELP
        }

        if (!mute) {
            println(lastHelpMessage)
        }
    }

    private fun printMainHelpAndExit() {
        System.err.println(MAIN_HELP)
        exitProcess(0)
    }

    private fun dispatchHelp(argv0: Array<String>) {
        var i = 0
        while (i < argv0.size) {
            val arg = argv0[i]

            if (arg.equals("scrape", true)) scrape = true
            if (arg.equals("harvest", true)) harvest = true
            if (arg.equals("arrange", true)) arrange = true
            if (arg.equals("sql", true)) sql = "help"
            if (arg.equals("serve", true)) server = true
            if (arg.equals("server", true)) server = true

            if (arg in listOf("-v", "-verbose")) {
                helpVerbose = true
            }

            ++i
        }

        when {
            scrape -> {
                lastHelpMessage = formatOptionHelp(scrapeOptions())
            }
            arrange -> {
                lastHelpMessage = "Detect the url groups"
            }
            harvest -> {
                lastHelpMessage = formatOptionHelp(harvestOptions())
            }
            sql == "help" -> {
                lastHelpMessage = formatXSQLHelp(xsqlHelp())
            }
            server -> lastHelpMessage = "Run the Exotic server and web console"
            else -> lastHelpMessage = MAIN_HELP
        }
    }

    internal fun scrape(): List<Map<String, String?>> {
        val (portalUrl, args) = UrlUtils.splitUrlArgs(configuredUrl)
        if (!UrlUtils.isStandard(portalUrl)) {
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
            lastOutput = gson.toJson(results[0])
            output()
        } else {
            lastOutput = gson.toJson(results)
            output()
        }

        return results
    }

    private fun output() {
        if (!mute) {
            println(lastOutput)
        }
    }

    internal fun arrange() {
        val (portalUrl, args) = UrlUtils.splitUrlArgs(configuredUrl)
        if (!UrlUtils.isStandard(portalUrl)) {
            System.err.println("The portal url is invalid")
            return
        }

        runBlocking {
            val harvester = VerboseHarvester()
            val groups = harvester.arrangeLinks(configuredUrl)
            harvester.printAllAnchorGroups(groups)
        }
    }

    internal fun harvest() {
        val (portalUrl, args) = UrlUtils.splitUrlArgs(configuredUrl)
        if (!UrlUtils.isStandard(portalUrl)) {
            System.err.println("The portal url is invalid")
            return
        }

        runBlocking {
            VerboseHarvester().harvest(portalUrl, args)
        }
    }

    internal fun executeSQL() {
        val context = ScentSQLContexts.create()
        val rs = context.executeQuery(sql)
        lastOutput = ResultSetFormatter(rs, withHeader = true, asList = true).toString()
        // remove all ` character so that we can paste the sql in a terminal
        lastOutput = lastOutput?.replace("`", "")
        output()
    }

    internal fun runServer() {
        SpringApplicationBuilder(StandaloneApplication::class.java)
            .profiles("h2")
            .initializers(ScentContextInitializer())
            .registerShutdownHook(true)
            .run(*argv)
    }

    internal fun parseCmdLine() {
        if (parsed) {
            return
        }
        parsed = true

        var i = 0
        while (i < argv.size) {
            val arg = argv[i]
            val isLastArg = i == argv.size - 1

            if (arg == "harvest") {
                if (isLastArg) {
                    criticalHelp = true
                    break
                }

                harvest = true
                configuredUrl = argv.drop(i + 1).joinToString(" ")
                break
            } else if (arg == "arrange") {
                if (isLastArg) {
                    criticalHelp = true
                    break
                }

                arrange = true
                configuredUrl = argv.drop(i + 1).joinToString(" ")
                break
            } else if (arg == "scrape") {
                if (isLastArg) {
                    criticalHelp = true
                    break
                }

                scrape = true
                configuredUrl = argv.drop(i + 1).joinToString(" ")
                parseScrapeArgs()
                break
            } else if (arg == "server" || arg == "serve") {
                server = true
                break
            } else if (arg == "sql") {
                if (isLastArg) {
                    criticalHelp = true
                    break
                }

                sql = argv.drop(i + 1).joinToString(" ")
                break
            }

            if (arg == "-headless") {
                headless = true
            }
            if (arg in listOf("?", "-h", "-help")) {
                criticalHelp = true
                break
            }
            if (arg == "--help") {
                help = true
                if (!isLastArg) {
                    helpArgs = argv.drop(i + 1).toTypedArray()
                }
                break
            }

            ++i
        }
    }

    private fun parseScrapeArgs() {
        val argv = configuredUrl.split(" ")
        var i = 0
        while (i < argv.size - 1) {
            if (argv[i] == "-field") scrapeFields.add(argv[++i])
            ++i
        }
    }

    private fun xsqlHelp(): ResultSet {
        return ScentSQLContexts.create().executeQuery("CALL XSQL_HELP()")
    }

    private fun formatXSQLHelp(rs: ResultSet): String {
        val sb = StringBuilder()
        rs.beforeFirst()
        while (rs.next()) {
            val function = rs.getString("XSQL FUNCTION")
            val namespace = rs.getString("NAMESPACE").takeIf { it.isNotBlank() } ?: "(Global)"
            val nativeFunction = rs.getString("NATIVE FUNCTION")
            val description = rs.getString("DESCRIPTION").takeIf { it.isNotBlank() } ?: "-"

            val message = """
$function: 
    $description
            """.trimIndent()

            val verboseMessage = """
$function:
    namespace:
        $namespace
    native function signature:
        $nativeFunction
    description:
        $description

            """.trimIndent()

            if (helpVerbose) {
                sb.appendLine(verboseMessage)
            } else {
                sb.appendLine(message)
            }
        }
        return sb.toString()
    }

    private fun scrapeOptions(): ResultSet {
        val rs = ResultSets.newSimpleResultSet("GROUP", "OPTION", "TYPE", "DEFAULT", "DESCRIPTION")
        LoadOptions.helpList.forEach { rs.addRow("LOAD OPTION", *it.toTypedArray()) }
        return rs
    }

    private fun formatOptionHelp(rs: ResultSet): String {
        val sb = StringBuilder()
        rs.beforeFirst()
        while (rs.next()) {
            val group = rs.getString("GROUP")
            val option = rs.getString("OPTION")
            val type = rs.getString("TYPE")
            val defaultValue = rs.getString("DEFAULT")
            val description = rs.getString("DESCRIPTION").takeIf { it.isNotBlank() } ?: "-"

            val message = """
$option: $type $defaultValue
    $description
            """.trimIndent()

            val verboseMessage = """
$option:
    group:
        $group
    type:
        $type
    default:
        $defaultValue
    description:
        $description

            """.trimIndent()

            if (helpVerbose) {
                sb.appendLine(verboseMessage)
            } else {
                sb.appendLine(message)
            }
        }
        return sb.toString()
    }

    private fun harvestOptions(): ResultSet {
        val rs = ResultSets.newSimpleResultSet("GROUP", "OPTION", "TYPE", "DEFAULT", "DESCRIPTION")
        HarvestOptions.helpList.forEach { rs.addRow("HARVEST OPTION", *it.toTypedArray()) }
        return rs
    }

    companion object {
        val MAIN_HELP = ResourceLoader.readString("help/main.txt")  +
                "\n\n" + ResourceLoader.readString("help/examples.txt")
    }
}
