package ai.platon.exotic.driver.crawl.entity

import ai.platon.exotic.driver.crawl.scraper.TaskStatus
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import javax.persistence.*

@Table(name = "page_tasks")
@Entity
@EntityListeners(AuditingEntityListener::class)
class PageTask(
    var url: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @ManyToOne
    var portalTask: PortalTask? = null

    var retryCount: Int = 0

    var startTime: Instant = Instant.now()

    var status: TaskStatus = TaskStatus.CREATED

    @CreatedDate
    var createdTime: Instant = Instant.now()

    @LastModifiedDate
    var lastModifiedTime: Instant = Instant.now()
}
