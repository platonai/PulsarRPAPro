package ai.platon.exotic.driver.crawl.entity

import ai.platon.exotic.driver.common.EPOCH_LDT
import ai.platon.exotic.driver.common.DOOMSDAY
import ai.platon.exotic.driver.common.NameGenerator
import ai.platon.exotic.driver.crawl.scraper.RuleStatus
import ai.platon.pulsar.common.DateTimes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.format.annotation.DateTimeFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
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

    @Column(name = "crawl_history", length = 1024)
    var crawlHistory: String = ""

    @Column(name = "period")
    var period: Duration = Duration.ofDays(3650)

    @Column(name = "cron_expression")
    var cronExpression: String? = null

    /**
     * Enum: Created, Running, Paused
     * */
    @Column(name = "status", length = 8)
    var status: String = RuleStatus.Created.toString()

    /**
     * TODO: use DateTimes.zoneOffset for the default value
     * The time difference, in minutes, between UTC time and local time.
     * */
    @Column(name = "timezone_offset")
    var timezoneOffset: Int = -480

    @CreatedDate
    @Column(name = "created_date")
    var createdDate: Instant = Instant.EPOCH

    @LastModifiedDate
    @Column(name = "last_modified_date")
    var lastModifiedDate: Instant = Instant.EPOCH

//    @OneToMany(fetch = FetchType.LAZY)
    @OneToMany(mappedBy = "rule", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    val portalTasks: MutableList<PortalTask> = mutableListOf()

//    fun zoneOffset() = ZoneOffset.ofHoursMinutes(0, timezoneOffset)

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

    /**
     * TODO: use EntityListeners
     * */
    final fun adjustFields() {
        period = period.truncatedTo(ChronoUnit.MINUTES)
        startTime = startTime.truncatedTo(ChronoUnit.SECONDS)
        lastCrawlTime = lastCrawlTime.truncatedTo(ChronoUnit.SECONDS)
        createdDate = createdDate.truncatedTo(ChronoUnit.SECONDS)
        lastModifiedDate = lastModifiedDate.truncatedTo(ChronoUnit.SECONDS)

        val count = cronExpression?.split(" ") ?: 0
        if (count == 5) {
            cronExpression = "0 $cronExpression"
        }

        if (startTime == Instant.EPOCH) {
            startTime = lastCrawlTime
        }

        name = name.takeIf { it.isNotBlank() } ?: randomName()
        label = label ?: ""
    }
}