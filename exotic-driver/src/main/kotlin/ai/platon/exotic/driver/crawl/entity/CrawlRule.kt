package ai.platon.exotic.driver.crawl.entity

import ai.platon.exotic.driver.common.EPOCH_LDT
import ai.platon.exotic.driver.common.DOOMSDAY
import ai.platon.pulsar.common.DateTimes
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Duration
import java.time.LocalDateTime
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
    var startTime: LocalDateTime = EPOCH_LDT.truncatedTo(ChronoUnit.SECONDS)

    @Column(name = "dead_time")
    var deadTime: LocalDateTime = DOOMSDAY

    @Column(name = "last_crawl_time")
    var lastCrawlTime: LocalDateTime = EPOCH_LDT.truncatedTo(ChronoUnit.SECONDS)

    @Column(name = "crawl_history", length = 1024)
    var crawlHistory: String = ""

    @Column(name = "period")
    var period: Duration = Duration.ofDays(3650)

    /**
     * Enum: Created, Running, Paused
     * */
    @Column(name = "status", length = 8)
    var status: String = "Created"

    @CreatedDate
    @Column(name = "created_date")
    var createdDate: LocalDateTime = EPOCH_LDT

    @LastModifiedDate
    @Column(name = "last_modified_date")
    var lastModifiedDate: LocalDateTime = EPOCH_LDT

    @OneToMany(fetch = FetchType.LAZY)
    val portalTasks: MutableList<PortalTask> = mutableListOf()

    fun buildArgs(): String {
        val taskTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        val formattedTime = DateTimes.format(taskTime, "YYMMddHH")
        val taskIdSuffix = id ?: formattedTime
        val taskId = "r$taskIdSuffix"
        var args = "-taskId $taskId -taskTime $taskTime"
        if (deadTime != DOOMSDAY) {
            args += " -deadTime $deadTime"
        }
        return args
    }

    final fun randomName(): String {
        return "T" + RandomStringUtils.randomAlphanumeric(6).lowercase()
    }

    final fun adjustFields() {
        period = period.truncatedTo(ChronoUnit.MINUTES)
        startTime = startTime.truncatedTo(ChronoUnit.SECONDS)
        lastCrawlTime = lastCrawlTime.truncatedTo(ChronoUnit.SECONDS)
        createdDate = createdDate.truncatedTo(ChronoUnit.SECONDS)
        lastModifiedDate = lastModifiedDate.truncatedTo(ChronoUnit.SECONDS)

        if (startTime == EPOCH_LDT) {
            startTime = lastCrawlTime
        }

        name = name.takeIf { it.isNotBlank() } ?: randomName()
        label = label ?: ""
    }
}
