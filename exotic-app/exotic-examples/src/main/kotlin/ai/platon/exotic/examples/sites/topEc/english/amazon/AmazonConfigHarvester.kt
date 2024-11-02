package ai.platon.exotic.examples.sites.topEc.english.amazon

import ai.platon.scent.tools.HarvesterProjectRunner

fun main() {
    val json = """
{
    "portalUrl": "https://www.amazon.com/Best-Sellers-Computers-Accessories-Laptop-Computers/zgbs/pc/565108/ref=zg_bs_nav_pc_1",
    "args": "-i 10d -ii 700d -parse -ol a[href~=/dp/] -tl 1000 -requireSize 1000000 -requireItemSize 2000000 -ignoreFailure -trustSamples -diagnosis",
    "itemURLNormalizer": {
        "urlCapturer": "https://.+.amazon.com(/.+)?/dp/(.+?)(/.+)?",
        "normalizedURLTemplate": "https://www.amazon.com/dp/{2}",
        "urlFilter": ".+amazon.+/dp/.+"
    }
}
"""

    val harvester = HarvesterProjectRunner.harvest(json)
    harvester.openViewDir()
}
