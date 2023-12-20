package ai.platon.exotic.crawl.common

import ai.platon.pulsar.common.OpenMapTable
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.model.createImage
import ai.platon.pulsar.dom.nodes.forEachElement
import ai.platon.pulsar.dom.nodes.node.ext.isImage
import ai.platon.pulsar.dom.nodes.node.ext.location
import ai.platon.pulsar.dom.nodes.node.ext.ownerDocument
import ai.platon.pulsar.dom.nodes.node.ext.removeAttrsIf
import ai.platon.scent.analysis.corpus.VisualDocument
import ai.platon.scent.analysis.corpus.visualDocument
import ai.platon.scent.dom.nodes.node.ext.isLocallyConstant
import ai.platon.scent.dom.nodes.node.ext.isSimpleTable
import ai.platon.scent.entities.PageTableGroup
import ai.platon.scent.entities.data
import ai.platon.scent.view.builder.TableViewBuilder
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Element
import java.time.LocalDateTime
import kotlin.collections.set

class ExoticPageTableViewBuilder(
        val tableGroup: PageTableGroup,
        devMode: Boolean = false
): TableViewBuilder(devMode) {
    companion object {
        private const val resource = "wwwroot/template/exotic.page.table.html"
        private val template: String = ResourceLoader.readString(resource)
    }

    private val tables = tableGroup.tables
    private lateinit var sampleTable: OpenMapTable
    private lateinit var documents: List<VisualDocument>

    fun build(): FeaturedDocument {
        val output = Documents.parse(template, "")

        output.document.outputSettings().prettyPrint(false)

        val messageView = output.body.selectFirst("div#page-message")!!

        sampleTable = tables.lastOrNull()?:OpenMapTable(0)
        documents = sampleTable.rows.mapNotNull { it.data.component?.ownerDocument?.visualDocument }
        val sampleDocument = documents.firstOrNull()?: return FeaturedDocument.NIL

        val domain = URLUtil.getDomainName(sampleDocument.location)
        val documentSize = sampleTable.data.documentSize

        output.document.title("PLATON AI - Auto Extract - $domain")
        val caption = String.format("Total %d pages, generated %d tables", documentSize, tables.size)
        messageView.appendElement("div").text(caption)
        messageView.appendElement("div").text(LocalDateTime.now().toString())

        messageView.appendElement("hr")
        messageView.appendElement("div").text(tableGroup.command)

        val tableListView = output.body.selectFirst("div#tables")!!
        // Tables extracted from this corpus
        tables.sortedBy { it.data.alignedTop }.forEachIndexed { i, table ->
            val tableData = table.data

            val message = "<div class='message'>" +
                    "<i class='no'>${1 + i}.</i>e<b>X</b>tracted <em>${table.numColumns - 2}</em> fields " +
                    "from page area <b>${table.data.name}</b></div>"
            tableListView.appendElement("div").html(message)
            tableListView.append(buildPageTable(table))

            if (devMode && !tableData.isCombined) {
                buildGeneratedHyperPaths(tableListView, table)
                tableListView.appendElement("br")
                buildGeneratedXSQL(tableListView, table)
                tableListView.appendElement("br")
                buildMLStatistics(tableListView, table)
            }

            tableListView.appendElement("br")
        }

        val naturalComponentsView = output.body.selectFirst("div#natural-components")!!
        buildNaturalComponents(naturalComponentsView)

        return output
    }

    private fun buildMLStatistics(rootElement: Element, table: OpenMapTable) {
        val div = rootElement.appendElement("div")
                .addClass("-hidden")
                .addClass("statistics")
        div.appendElement("h2").text("Machine Learning Statistics")

        val dl = div.appendElement("dl")
        val d = table.data
        arrayOf("score" to d.score, "distortion" to d.distortion,
                "macroP" to d.macroP, "macroR" to d.macroR, "macroF1" to d.macroF1,
                "microP" to d.microP, "microR" to d.microR, "microF1" to d.microF1,
                "ff" to d.ff, "ffr" to d.ffr)
                .forEach { (name, value) -> dl.append("<dt>$name</dt><dd>$value</dd>") }
    }

    private fun buildGeneratedHyperPaths(rootElement: Element, table: OpenMapTable) {
        val div = rootElement.appendElement("div")
                .addClass("-hidden")
                .addClass("css-paths")
        div.appendElement("h2").text("Generated CSS Paths")

        val dl = div.appendElement("dl")
        table.columns.forEach { column ->
            column.data.hyperPath.takeIf { it.isNotBlank() }?.let {
                dl.append("<dt>${column.name}</dt><dd>${it}</dd>")
            }
        }
    }

    private fun buildGeneratedXSQL(rootElement: Element, table: OpenMapTable) {
        val restrictPath = table.data.hyperPath
        val sql = table.data.xsql
            .replace(restrictPath, "body")
            .replace("`", "")
            .trim()

        val div = rootElement.appendElement("div")
            .addClass("-hidden")
            .addClass("x-sql")
            .addClass("language-sql")
        div.appendElement("h2").text("Generated X-SQL")

        div.append(
"""
<pre><code class='language-bash'>java -jar exotic-standalone*.jar sql "
$sql
"</code></pre>
"""
        )
    }

    private fun buildNaturalComponents(naturalComponentsView: Element) {
        val naturalComponents = documents.flatMap {
            it.naturalComponents.filter {
                it.element.isSimpleTable && !it.element.isLocallyConstant
            }
        }
        naturalComponents.groupBy { it.uniquePath }.forEach { (uniquePath, components) ->
            naturalComponentsView.appendElement("br")
            naturalComponentsView.appendElement("hr")
            naturalComponentsView.appendElement("h2").text("Natural component:")
            naturalComponentsView.appendElement("div").addClass("message").text(uniquePath)

            val rawView = naturalComponentsView.appendElement("div")
            components.forEachIndexed { j, it ->
                val clone = it.element.clone()
                clone.removeAttrsIf { it.key !in arrayOf("id", "class", "src", "href") }
                clone.forEachElement {
                    it.removeAttrsIf { it.key !in arrayOf("id", "class", "src", "href") }
                }

                rawView.appendElement("br")
                rawView.appendElement("hr")
                rawView.appendElement("h3").text("${1 + j}.")
                rawView.appendElement("div").appendElement("a")
                        .attr("href", it.element.baseUri())
                        .text(it.element.ownerDocument.title())
                rawView.appendChild(clone)
            }
        }
    }

    private fun buildLinkSnapshot(contentView: Element) {
        if (tables.isEmpty()) {
            return
        }

        val sampleSize = 10
        val table = tables.first()
        val samples = table.rows.take(sampleSize)
        // val samples = documents.take(sampleSize)
        val sampleUrls = samples.map { it.data.location }

        contentView.appendElement("div").text("Show ${samples.size}/${table.numRows} pages: ")

        buildLinkSnapshot(contentView, samples)
    }

    private fun buildLinkSnapshot(contentView: Element, rows: Collection<OpenMapTable.Row>) {
        val ul = contentView.appendElement("ul")
        rows.forEachIndexed { i, row ->
            val uri = row.data.location
            val title = row.data.title

            val li = ul.appendElement("li")
            li.appendElement("span").html("${1 + i}.&nbsp;")
            li.appendElement("a")
                    .attr("href", uri)
                    .attr("target", "_blank")
                    .text(title)
        }
    }

    private fun buildLinkSnapshot(root: Element, elements: Iterable<Element>) {
        val ul = root.appendElement("ul")
        elements.forEachIndexed { i, it ->
            val li = ul.appendElement("li")
            li.appendElement("span").html("${1 + i}.&nbsp;")
            li.appendElement("a")
                    .attr("href", it.location)
                    .attr("target", "_blank")
                    .text(it.ownerDocument()!!.title())

            if (it.isImage) {
                val image = createImage(it)
                image.attributes["width"] = "200"
                image.attributes["height"] = "200"
                li.appendElement(image.toString())
            }
        }
    }

    private fun buildLinkTable(root: Element, n: Int = 50) {
        if (tables.isEmpty()) {
            return
        }

        val table = tables.first()
        val rows = table.rows.take(n)

        root.appendElement("hr")
        root.appendElement("div").text("Show ${rows.size}/${table.rows.size} pages: ")
        val tableElement = root.appendElement("table")

        var tr = tableElement.appendElement("tr")
        tr.append("<th>&nbsp;</th><th>Title</th><th>Url</th>")

        rows.forEachIndexed { i, row ->
            val uri = row.data.location
            val displayUri = StringUtils.abbreviateMiddle(row.data.location, "..", 200)
            val exportPath = row.data.exportPath
            val title = row.data.title

            tr = tableElement.appendElement("tr")

            tr.appendElement("td").text("${1 + i}")

            tr.appendElement("td").appendElement("a")
                    .attr("href", exportPath.toString())
                    .attr("target", "_blank")
                    .text(title)

            tr.appendElement("td").appendElement("a")
                    .attr("href", uri)
                    .attr("target", "_blank")
                    .text(displayUri)
        }
    }
}
