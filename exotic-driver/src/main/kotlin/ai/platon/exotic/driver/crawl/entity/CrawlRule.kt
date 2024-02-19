package ai.platon.exotic.driver.crawl.entity

import ai.platon.exotic.common.ExoticUtils
import ai.platon.exotic.common.NameGenerator
import ai.platon.exotic.driver.crawl.scraper.RuleStatus
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.urls.UrlUtils
import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.format.annotation.DateTimeFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*
import javax.persistence.*

@Table(name = "crawl_rules")
@Entity
@EntityListeners(AuditingEntityListener::class)
class CrawlRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @Column(name = "name", length = 32)
    var name: String = randomName()

    @Column(name = "label", length = 64)
    var label: String? = null

    @Lob
    @Column(name = "portal_urls")
    var portalUrls: String = ""

    @Column(name = "out_link_selector", length = 64)
    var outLinkSelector: String? = null

    @Lob
    @Column(name = "sql_template")
    var sqlTemplate: String? = null

    @Column(name = "description", length = 128)
    var description: String? = null

    @Column(name = "next_page_selector", length = 64)
    var nextPageSelector: String? = null

    @Column(name = "max_pages")
    var maxPages: Int = 30

    @Column(name = "start_time")
    @DateTimeFormat(pattern = "yyyy-MM-ddTHH:mm:ssZ")
    var startTime: Instant = Instant.EPOCH

    @Column(name = "dead_time")
    @DateTimeFormat(pattern = "yyyy-MM-ddTHH:mm:ssZ")
    var deadTime: Instant = DateTimes.doomsday

    @Column(name = "last_crawl_time")
    var lastCrawlTime: Instant = Instant.EPOCH

    @Column(name = "start_count")
    var crawlCount: Int? = 0

    @Column(name = "crawl_history", length = 1024)
    var crawlHistory: String = ""

    @Column(name = "period")
    var period: Duration = Duration.ofDays(3650)
    
    @Column(name = "priority")
    var priority: String? = Priority13.LOWER2.toString()

    @Column(name = "cron_expression")
    var cronExpression: String? = null

    /**
     * Enum: Created, Running, Paused
     * */
    @Column(name = "status", length = 8)
    var status: String = RuleStatus.Created.toString()

    /**
     * The time difference, in minutes, between UTC time and local time.
     * */
    @Column(name = "timezone_offset_minutes")
    var timezoneOffsetMinutes: Int? = -480
    @CreatedDate
    @Column(name = "created_date")
    var createdDate: Instant = Instant.now()

    @LastModifiedDate
    @Column(name = "last_modified_date")
    var lastModifiedDate: Instant = Instant.now()

//    @OneToMany(fetch = FetchType.LAZY)
    @OneToMany(mappedBy = "rule", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    val portalTasks: MutableList<PortalTask> = mutableListOf()

    val zoneOffset: ZoneOffset
        get() {
            val minutes = timezoneOffsetMinutes ?: -480
            return ZoneOffset.ofHoursMinutes(minutes / 60, minutes % 60)
        }

    val portalUrlList get() = portalUrls.split("\n")
        .filter { it.isNotBlank() }
        .filter { UrlUtils.isStandard(it) }

    val descriptivePeriod: String
        get() {
            val expression = cronExpression
            return when {
                period.isNegative && expression != null -> describeCron(expression)
                period.toDays() > 360 -> "once"
                else -> "every " + ExoticUtils.formatDuration(period.seconds)
            }
        }

    val deducedDomain: String
        get() {
            val host = portalUrlList.firstOrNull()?.let { UrlUtils.getURLOrNull(it) }?.host
            if (host != null) {
                // TODO: use URLUtil.getDomainName
                val parts = host.split(".")
                return if (parts[0] == "www") {
                    parts.drop(1).joinToString(".")
                } else {
                    parts.takeLast(2).joinToString(".")
                }
            }
            return "-"
        }

    val localCreatedDateTime: LocalDateTime
        get() = createdDate.atOffset(zoneOffset).toLocalDateTime()

    val localLastModifiedDateTime: LocalDateTime
        get() = lastModifiedDate.atOffset(zoneOffset).toLocalDateTime()

    val parsedPriority get() = Priority13.valueOfOrNull(priority ?: "") ?: Priority13.LOWER2
    
    fun buildArgs(): String {
        val taskTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        val formattedTime = DateTimes.format(taskTime, "YYMMddHH")
        val taskIdSuffix = id ?: formattedTime
        val taskId = "r$taskIdSuffix"
        var args = "-taskId $taskId -taskTime $taskTime"
        if (deadTime != DateTimes.doomsday) {
            args += " -deadTime $deadTime"
        }
        return args
    }

    final fun randomName(): String {
        return NameGenerator.gen()
    }

    private fun describeCron(expression: String): String {
        val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ)
        val parser = CronParser(cronDefinition)
        val quartzCron: Cron = parser.parse(expression)
        val descriptor = CronDescriptor.instance(Locale.getDefault())
        return descriptor.describe(quartzCron)
    }

    @PrePersist
    @PreUpdate
    @PostLoad
    final fun adjustFields() {
        val count = cronExpression?.split(" ") ?: 0
        if (count == 5) {
            cronExpression = "0 $cronExpression"
        }

        name = name.takeIf { it.isNotBlank() } ?: randomName()
    }
}
