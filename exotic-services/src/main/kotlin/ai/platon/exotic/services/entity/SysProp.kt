package ai.platon.exotic.services.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Table(name = "sys_props")
@Entity
class SysProp(
    @Id
    @Column(nullable = false, length = 32)
    var name: String,
    var value: String? = null
)
