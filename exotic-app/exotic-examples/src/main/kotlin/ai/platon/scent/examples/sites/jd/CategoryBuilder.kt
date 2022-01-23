package ai.platon.scent.examples.sites.jd

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.dom.nodes.node.ext.cleanText
import ai.platon.pulsar.dom.select.selectFirstOrNull
import org.jsoup.nodes.Element

class CategoryNode(
    val name: String,
    val url: String,

    var parentNode: CategoryNode? = null
) {
    val children = mutableListOf<CategoryNode>()

    val depth: Int get() {
        var d = 0
        var p = parentNode
        while (p != null) {
            ++d
            p = p.parentNode
        }
        return d
    }

    fun toAnchor(): String {
        val numChildren = children.size
        return "<a href='$url' data-depth=$depth data-children=$numChildren>$name</a>"
    }

    override fun toString(): String {
        val d = depth
        val margin = "\t".repeat(depth)
        return "$margin[$d]$name -> $url"
    }
}

class CategoryBuilder {

    fun parseCategoryTree(): CategoryNode {
        val categoryHtml = ResourceLoader.readString("sites/jd/categories.jd.html")
        val doc = Documents.parse(categoryHtml, "https://www.jd.com/")

        val categoryTree = CategoryNode("Root", "")
        doc.select("ul.cate_menu > li.cate_menu_item").mapIndexed { i, itemEle ->
            val dataIndex = itemEle.attr("data-index").toInt()
            val j = dataIndex
            val name = itemEle.cleanText
            val anchor = itemEle.selectFirst("a")
            anchor.text(name)
            val topCategory = createNode(anchor, categoryTree)
            // println("$j.$topCategory")

            val elements = doc.selectFirstOrNull("#cate_item$j")
                ?.select(".cate_detail .cate_detail_item")
            elements?.forEach { ele ->
                val e2 = ele.selectFirstOrNull("dt a")
                if (e2 != null) {
                    val level2Category = createNode(e2, topCategory)
                    // println("$level2Category")

                    ele.select("dd a").forEach { e3 ->
                        val level3Category = createNode(e3, level2Category)
                        // println("$level3Category")
                    }
                }
            }
        }

        return categoryTree
    }

    fun createNode(ele: Element, parentNode: CategoryNode? = null): CategoryNode {
        val name = ele.ownText().trim()
        val url = ele.absUrl("href")

        val category = CategoryNode(name, url, parentNode)
        parentNode?.children?.add(category)
        return category
    }

    fun formatHtml(node: CategoryNode): StringBuilder {
        val sb = StringBuilder()
        sb.appendLine("<ul>")
        formatHtml0(node, sb)
        sb.appendLine("</ul>")

        return sb
    }

    private fun formatHtml0(node: CategoryNode, sb: StringBuilder) {
        val depth = node.depth
        val margin = " ".repeat(2 * depth)
        val name = node.name
        val anchor = node.toAnchor()
        // println("$margin<li>$anchor</li>")
        sb.appendLine("$margin<li>")
        sb.appendLine("$margin$anchor")
        if (node.children.isNotEmpty()) {
            // println("$margin<ul>")
            sb.appendLine("$margin<ul data-name='$name'>")
            node.children.forEach {
                formatHtml0(it, sb)
            }
            // println("$margin</ul>")
            sb.appendLine("$margin</ul>")
        }
        sb.appendLine("$margin</li>")
    }

    fun format(node: CategoryNode) {
        println("$node")
        node.children.forEach {
            format(it)
        }
    }
}

fun main() {
    val builder = CategoryBuilder()
    val categoryTree = builder.parseCategoryTree()
    // builder.format(categoryTree)
    val sb = builder.formatHtml(categoryTree)
    println(sb.toString())
}
