package ai.platon.exotic.examples.sites.topEc.english.amazon

import ai.platon.exotic.crawl.common.VerboseHarvester
import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.scent.common.MLPaths
import ai.platon.scent.ml.encoding.EncodeProject
import ai.platon.scent.ml.harvest.HarvestProject
import ai.platon.scent.ql.h2.context.ScentSQLContexts

fun main() {
    val projectId = "p20241030"
    val project = EncodeProject(projectId, EncodeProject.Type.TRAINING)

    val portalUrl = "https://www.amazon.com/Best-Sellers/zgbs"
    val args = "-projectId ${project.id} -i 1s -ii 7s -ol a[href~=/dp/] -tl 1000 -requireSize 1000000 -ignoreFailure -diagnosis"

    val session = ScentSQLContexts.createSession()
    val options = session.options(args)
    options.itemEvent.loadEventHandlers.onLoaded.addLast { page ->
        val fileName = AppPaths.fromUri(page.url, "", ".html")
        session.exportTo(page, project.htmlBaseDir.resolve(fileName))
    }
    // session.submitForOutPages(portalUrl, options)

    PulsarContexts.await()

    val harvester = VerboseHarvester()
    harvester.harvest(project.id)

    val harvestProject = HarvestProject(projectId)
    harvester.openBrowser(harvestProject.resultBaseDir)
}
