package ai.platon.exotic.services.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "sys_props")
@Entity
class SysProp(
    @Id
    @Column(nullable = false, length = 32)
    var name: String,
    var value: String? = null
)
