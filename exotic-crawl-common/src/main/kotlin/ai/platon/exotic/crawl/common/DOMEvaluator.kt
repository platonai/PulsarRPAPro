package ai.platon.exotic.crawl.common

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.math.vectors.get
import ai.platon.pulsar.dom.nodes.node.ext.*
import ai.platon.scent.common.message.ScentMiscMessageWriter
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.ml.EncodeOptions
import ai.platon.scent.ml.NodePoint
import ai.platon.scent.ml.jpmml.JPMMLEvaluator
import com.google.common.base.CharMatcher
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DOMEvaluator(
    modelPath: Path,
    private val predictResultPath: Path? = null
) {
    companion object {
        const val FULL_WIDTH_COMMA = "，"
    }

    private val logger = getLogger(this)

    private val session = ScentContexts.createSession()

    private val evaluator = JPMMLEvaluator()

    private val messageWriter = session.context.getBean<ScentMiscMessageWriter>()

    private val defaultArgs = "" +
            " -nScreens 1" +
            " -trustSamples" +
//                " -polysemous" +
            " -diagnose" +
            " -nVerbose 1" +
            " -showTip" +
            " -showImage" +
//                " -cellType PLAIN_TEXT"
            ""

    private val datasetPath = Files.createTempFile("dataset-", "csv")
    
    private val biddingNodeFilter: (Node) -> Boolean = { node ->
        node is Element &&
                node.left in 0..500 &&
                node.width >= 200 &&
                node.top in 100..2000 &&
                node.bottom > 100 &&
                node.numChars > 5
    }

    private val expectedFeatures = """
top-g0,top-g1,top-g2,top-g3,left-g0,left-g1,left-g2,left-g3,width-g0,width-g1,width-g2,width-g3,height-g0,height-g1,height-g2,height-g3,char-g0,char-g1,char-g2,char-g3,txt_nd-g0,txt_nd-g1,txt_nd-g2,txt_nd-g3,img-g0,img-g1,img-g2,img-g3,a-g0,a-g1,a-g2,a-g3,sibling-g0,sibling-g1,sibling-g2,sibling-g3,child-g0,child-g1,child-g2,child-g3,dep-g0,dep-g1,dep-g2,dep-g3,seq-g0,seq-g1,seq-g2,seq-g3,txt_dns-g0,txt_dns-g1,txt_dns-g2,txt_dns-g3,pid-g0,pid-g1,pid-g2,pid-g3,tag-g0,tag-g1,tag-g2,tag-g3,nd_id-g0,nd_id-g1,nd_id-g2,nd_id-g3,nd_cs-g0,nd_cs-g1,nd_cs-g2,nd_cs-g3,ft_sz-g0,ft_sz-g1,ft_sz-g2,ft_sz-g3,color-g0,color-g1,color-g2,color-g3,b_bolor-g0,b_bolor-g1,b_bolor-g2,b_bolor-g3,rtop-g0,rtop-g1,rtop-g2,rtop-g3,rleft-g0,rleft-g1,rleft-g2,rleft-g3,rrow-g0,rrow-g1,rrow-g2,rrow-g3,rcol-g0,rcol-g1,rcol-g2,rcol-g3,dist-g0,dist-g1,dist-g2,dist-g3,simg-g0,simg-g1,simg-g2,simg-g3,mimg-g0,mimg-g1,mimg-g2,mimg-g3,limg-g0,limg-g1,limg-g2,limg-g3,aimg-g0,aimg-g1,aimg-g2,aimg-g3,saimg-g0,saimg-g1,saimg-g2,saimg-g3,maimg-g0,maimg-g1,maimg-g2,maimg-g3,laimg-g0,laimg-g1,laimg-g2,laimg-g3,char_max-g0,char_max-g1,char_max-g2,char_max-g3,char_ave-g0,char_ave-g1,char_ave-g2,char_ave-g3,own_char-g0,own_char-g1,own_char-g2,own_char-g3,own_txt_nd-g0,own_txt_nd-g1,own_txt_nd-g2,own_txt_nd-g3,grant_child-g0,grant_child-g1,grant_child-g2,grant_child-g3,descend-g0,descend-g1,descend-g2,descend-g3,sep-g0,sep-g1,sep-g2,sep-g3,rseq-g0,rseq-g1,rseq-g2,rseq-g3,txt_nd_c-g0,txt_nd_c-g1,txt_nd_c-g2,txt_nd_c-g3,vcc-g0,vcc-g1,vcc-g2,vcc-g3,vcv-g0,vcv-g1,vcv-g2,vcv-g3,avcc-g0,avcc-g1,avcc-g2,avcc-g3,avcv-g0,avcv-g1,avcv-g2,avcv-g3,hcc-g0,hcc-g1,hcc-g2,hcc-g3,hcv-g0,hcv-g1,hcv-g2,hcv-g3,ahcc-g0,ahcc-g1,ahcc-g2,ahcc-g3,ahcv-g0,ahcv-g1,ahcv-g2,ahcv-g3,txt_df-g0,txt_df-g1,txt_df-g2,txt_df-g3,cap_df-g0,cap_df-g1,cap_df-g2,cap_df-g3,tn_max_w-g0,tn_max_w-g1,tn_max_w-g2,tn_max_w-g3,tn_ave_w-g0,tn_ave_w-g1,tn_ave_w-g2,tn_ave_w-g3,tn_max_h-g0,tn_max_h-g1,tn_max_h-g2,tn_max_h-g3,tn_ave_h-g0,tn_ave_h-g1,tn_ave_h-g2,tn_ave_h-g3,a_max_w-g0,a_max_w-g1,a_max_w-g2,a_max_w-g3,a_ave_w-g0,a_ave_w-g1,a_ave_w-g2,a_ave_w-g3,a_max_h-g0,a_max_h-g1,a_max_h-g2,a_max_h-g3,a_ave_h-g0,a_ave_h-g1,a_ave_h-g2,a_ave_h-g3,img_max_w-g0,img_max_w-g1,img_max_w-g2,img_max_w-g3,img_ave_w-g0,img_ave_w-g1,img_ave_w-g2,img_ave_w-g3,img_max_h-g0,img_max_h-g1,img_max_h-g2,img_max_h-g3,img_ave_h-g0,img_ave_h-g1,img_ave_h-g2,img_ave_h-g3,tn_total_w-g0,tn_total_w-g1,tn_total_w-g2,tn_total_w-g3,tn_total_h-g0,tn_total_h-g1,tn_total_h-g2,tn_total_h-g3,a_total_w-g0,a_total_w-g1,a_total_w-g2,a_total_w-g3,a_total_h-g0,a_total_h-g1,a_total_h-g2,a_total_h-g3,img_total_w-g0,img_total_w-g1,img_total_w-g2,img_total_w-g3,img_total_h-g0,img_total_h-g1,img_total_h-g2,img_total_h-g3
    """.trimIndent()

    private var predictCount = 0

    init {
        evaluator.model = modelPath.toFile()

        if (!Files.exists(evaluator.model.toPath())) {
            System.err.println("Model file not found | $modelPath")
        }
    }

    fun predict(record: Map<String, *>): Map<String, *> {
        return evaluator.evaluate(record)
    }

    fun predict(url: String): Map<String, *> {
        return try {
            predict0(url)
        } catch (t: Throwable) {
            t.printStackTrace()
            mapOf<String, Any>()
        }
    }

    private fun predict0(url: String): Map<String, *> {
        val options = session.options(defaultArgs)

        val document = session.loadDocument(url, options)
        if (document.isNil() || document.document.numChars < 1000) {
            logger.warn("Invalid document | {}", url)
            return mapOf<String, Any>()
        }
        
        val encodeOptions = EncodeOptions(datasetPath = datasetPath)
        val df = session.encodeNodes(document, encodeOptions, filter = biddingNodeFilter)
        val columns = df.schema.columns

        if (df.points.isEmpty()) {
            logger.warn("No point | {}", url)
            return mapOf<String, Any>()
        }
        
        val columnsNames = columns.joinToString(",") { it.name }
        if (columnsNames != expectedFeatures) {
            logger.info(columnsNames)
            logger.info("Column size: " + columns.size + " dimension: " + df.points[0].dimension)
            return mapOf<String, Any>()
        }

        var result: Map<String, *> = mapOf<String, Any>()
        val sb = StringBuilder()
        df.points.forEach { x ->
            val x1 = IntRange(0, columns.size - 1).associate { i -> columns[i].name to x[i] }
            result = predict(x1)

            val p = result["probability(1)"]
            if (p == 1.0) {
                toCSVRecord(sb, url, x, result)
                return@forEach
            }
        }

        if (predictResultPath != null && sb.isNotBlank()) {
            messageWriter.write(sb.toString(), predictResultPath)
        }
        sb.clear()

        return result
    }

    private fun toCSVRecord(sb: StringBuilder, url: String, x: NodePoint, result: Map<String, *>) {
        val ele = x.node.bestElement
        val title = ele.text()

//        val csvFormat = CSVFormat.newFormat(',')
//        csvFormat.format(url).format(title)

        sb.append(cleanCsvCell(url)).append(",")
        sb.append(cleanCsvCell(title)).append(",")

        val contentNode = ele.parents().firstOrNull {
            it.numChars > 200 && it.numTextNodes > 20 && it.numAnchors < 10
        }

        if (contentNode != null) {
            val content = contentNode.text()
            val abbreviateContent = StringUtils.abbreviateMiddle(content, "...", 100)
            sb.append(cleanCsvCell(abbreviateContent)).append(",")
            sb.append(cleanCsvCell(content))
        }
    }

    private fun cleanCsvCell(text: String): String {
        var text0 = text

        text0 = text0.replace("\\p{Cntrl}".toRegex(), "\t")
        text0 = CharMatcher.javaIsoControl().removeFrom(text0)
        text0 = text0.replace(",", FULL_WIDTH_COMMA)

        return text0
    }
}

fun main() {
    // http://platonic.fun/pub/ml/bidding/dom_decision_tree_bidding.0.0.1.pmml
    val modelPath = Paths.get("/tmp/dom_decision_tree.pmml")
    if (!Files.exists(modelPath)) {
        val modelURL = "http://platonic.fun/pub/ml/bidding/dom_decision_tree_bidding.0.0.1.pmml"
        FileUtils.copyURLToFile(URL(modelURL), modelPath.toFile())
    }
    val model = DOMEvaluator(modelPath)

    val urls = """
http://www.ccgp.gov.cn/cggg/zygg/gkzb/201811/t20181128_11209985.htm
http://www.infobidding.com/infobidding/upload2/html/info/2022/12/23/info_57986964.html
http://www.ccgp-xizang.gov.cn/freecms/site/xizang/xzcggg/info/2022/73046.html
https://jyzx.dh.gov.cn/jyxx/jsgcZbjggsDetail?guid=739ff5bc-a590-4fa5-86e2-c49b7f9b4961&isOther=false
https://ggzy.guizhou.gov.cn/tradeInfo/detailHtml?metaId=766181105433137152
https://www.plap.cn/index/selectArticleNewsById.do?id=E114E25B7E254B55B5F725A63ABFACE9
http://www.ccgp-xizang.gov.cn/freecms/site/xizang/xzcggg/info/2022/73281.html
https://ggzy.guizhou.gov.cn/tradeInfo/detailHtml?metaId=77698184
http://cg.gemas.com.cn/cgcgwgg/213173.jhtml
http://www.yulin.gov.cn/zwgk/ggzypzlygk/zfcgly/zbgg/t14505306.shtml
http://www.qlebid.com/cms/channel/1ywgg2fw/32828.htm
http://www.gzggzy.cn/jyywjsgcyllyzsjggs/916231.jhtml
http://www.hljggzyjyw.org.cn/jyfwdt/003002/003002001/003002001001/20221208/b5b310cc-ac7e-4251-87de-a594e9f89b22.html
http://47.96.125.164:8060/jcqgg/15380.jhtml
http://ggzyjy.lsz.gov.cn/TPFront/infodetail/?infoid=d8422a0a-6d40-40c3-9ba1-1755ed0c66f6&categoryNum=005001007
http://www.gzggzy.cn/jyywjsgcyllyzbhxrgs/917951.jhtml
http://ggb.sx.gov.cn/art/2022/11/18/art_1518855_58977140.html
http://ggzyjy.zgzhijiang.gov.cn/zjSite/InfoDetail/?InfoID=a8c26778-0cb6-4dd4-8429-5f9a7ffec3e5&CategoryNum=003001004003
http://ggzy.yueyang.gov.cn/56114/56143/56144/content_2020850.html
http://ggb.sx.gov.cn/art/2022/12/16/art_1518857_58978178.html
https://www.plap.cn/index/selectArticleNewsById.do?id=74D707E111BD413ABCECDCB23EE9324D
https://ggzy.yibin.gov.cn/Jyweb/JYXTXiangMuXinXiView.aspx?type=%e5%bb%ba%e8%ae%be%e5%b7%a5%e7%a8%8b&subtype=130&XXLY=2&Guid=71aac070-8bd0-4761-84a7-4664ca06673d
http://www.yulin.gov.cn/zwgk/ggzypzlygk/zfcgly/gzcq/t14010118.shtml
http://www.zmzb.com/cms/channel/ywgg4hw/21585.htm
http://ggzyjy.yichang.gov.cn/TPFront/infodetail/?infoid=1b12da6b-81c5-4759-bb07-5172806d1aeb&categoryNum=003002002001
http://www.ccgp-xizang.gov.cn/freecms/site/xizang/xzcggg/info/2022/73243.html
http://218.67.246.33/webInfo/getWebInfoByPkWebInfoId1.do?pkWebInfoId=0632C73E-6C24-493C-BDD5-9F5731FCC65D
http://www.whszfcg.com/wuhan/views/announce/announce_info.html?uuid=033cd1ce3f474b89939ff2a3646096a2&type=1
        """.trimIndent().split("\n").map { it.trim() }.filter { it.startsWith("http") }

    urls.take(100).forEach { url ->
        val result = model.predict(url)
        println(result)
    }
}
