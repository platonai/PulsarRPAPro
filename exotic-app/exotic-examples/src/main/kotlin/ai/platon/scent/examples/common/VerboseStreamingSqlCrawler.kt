package ai.platon.scent.examples.common

import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.proxy.ProxyVendorUntrustedException
import ai.platon.pulsar.persist.WebPage
import ai.platon.scent.ql.h2.context.ScentSQLContext
import kotlinx.coroutines.*
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

open class VerboseStreamingSqlCrawler(
        private val urls: Sequence<String>,
        private val options: LoadOptions,
        context: ScentSQLContext
): VerboseSqlExtractor(context) {
    companion object {
        private val numRunningTasks = AtomicInteger()
    }

    private val taskTimeout = Duration.ofMinutes(5)
    @Volatile
    private var numTasks = 0
    var onLoadComplete: (WebPage) -> Unit = {}

    open suspend fun run() {
        supervisorScope {
            urls.forEachIndexed { j, url ->
                ++numTasks

                var page: WebPage?
                var exception: Throwable? = null
                numRunningTasks.incrementAndGet()
                val context = Dispatchers.Default + CoroutineName("w")
                launch(context) {
                    withTimeout(taskTimeout.toMillis()) {
                        page = session.runCatching { loadDeferred(url, options) }
                                .onFailure { exception = it; log.warn("Load failed - $it") }
                                .getOrNull()
                        page?.also {
                            onLoadComplete(it)
                        }
                        numRunningTasks.decrementAndGet()
                    }
                }

                if (exception is ProxyVendorUntrustedException) {
                    log.error(exception?.message?:"Unexpected error")
                    return@supervisorScope
                }
            }
        }

        log.info("All done. Total $numTasks tasks")
    }
}
