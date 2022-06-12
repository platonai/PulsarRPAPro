package ai.platon.exotic.driver.crawl.scraper

import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.pulsar.driver.ScrapeResponse
import java.time.Duration
import java.time.Instant

enum class RuleStatus {
    Created,
    Running,
    Paused,
    Finished,
    Archived
}

enum class TaskStatus(
    private val value: Int,
    private val series: Series,
    val reasonPhrase: String
) {
    CREATED(201, Series.CREATED, "Created"),
    LOADED(202, Series.SUCCESSFUL, "Loaded"),
    SUBMITTED(203, Series.SUCCESSFUL, "Submitted"),
    ACCEPTED(203, Series.SUCCESSFUL, "Accepted"),
    OK(200, Series.FINISHED, "OK"),

    PROCESSING(301, Series.INFORMATIONAL, "Processing"),
    RETRYING(301, Series.INFORMATIONAL, "Retrying"),
    FAILED(501, Series.FINISHED, "Server Failed");

    enum class Series(private val value: Int) {
        CREATED(1), SUCCESSFUL(2), INFORMATIONAL(3), FINISHED(100)
    }
}

class ListenablePortalTask(
    val task: PortalTask,
    var refresh: Boolean = false,

    var onSubmitted: (ScrapeTask) -> Unit,
    var onSuccess: (ScrapeTask) -> Unit,
    var onFailed: (ScrapeTask) -> Unit,
    var onFinished: (ScrapeTask) -> Unit,
    var onRetry: (ScrapeTask) -> Unit,
    var onTimeout: (ScrapeTask) -> Unit,

    var onItemSubmitted: (ScrapeTask) -> Unit,
    var onItemSuccess: (ScrapeTask) -> Unit,
    var onItemFailed: (ScrapeTask) -> Unit,
    var onItemFinished: (ScrapeTask) -> Unit,
    var onItemRetry: (ScrapeTask) -> Unit,
    var onItemTimeout: (ScrapeTask) -> Unit,
) {
    companion object {
        fun create(task: PortalTask): ListenablePortalTask {
            return ListenablePortalTask(task, false, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        }
    }
}

class ScrapeTask constructor(
    val url: String,
    var args: String,
    var priority: Int,
    var sqlTemplate: String,
): Comparable<ScrapeTask> {
    var response: ScrapeResponse = ScrapeResponse()

    /**
     * The server side id
     * */
    var serverTaskId: String = ""

//    var parentId: String = "",
//    var parentUrl: String? = null,

    val createdTime: Instant = Instant.now()
    var lastCheckTime: Instant = Instant.EPOCH
    var timeout: Duration = Duration.ofHours(1)

    var submitCount = 0
    var collectedCount = 0
    var status: TaskStatus = TaskStatus.CREATED
    /**
     * The companion portal task if it's a portal task
     * */
    var companionPortalTask: PortalTask? = null
    val isPortal get() = companionPortalTask != null

    val nextCheckTime get() = lastCheckTime.plusSeconds(response.estimatedWaitTime.coerceAtMost(60))
    val shouldCheck get() = nextCheckTime < Instant.now()
    var collectedTime = Instant.EPOCH

    val isTimeout get() = Duration.between(createdTime, Instant.now()) > timeout

    var exceptionMessage: String? = null
    var exception: Exception? = null

    val configuredUrl get() = "$url $args"

    override fun equals(other: Any?): Boolean {
        return other is ScrapeTask && configuredUrl == other.configuredUrl
    }

    override fun hashCode(): Int {
        return configuredUrl.hashCode()
    }

    override fun compareTo(other: ScrapeTask): Int {
        return configuredUrl.compareTo(other.configuredUrl)
    }
}

class ListenableScrapeTask(
    val task: ScrapeTask
) {
    lateinit var onSubmitted: () -> Unit
    lateinit var onSuccess: () -> Unit
    lateinit var onFailed: () -> Unit
    lateinit var onFinished: () -> Unit
    lateinit var onRetry: () -> Unit
    lateinit var onTimeout: () -> Unit
}
