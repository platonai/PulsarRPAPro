package ai.platon.exotic.driver.common

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimes
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

val EPOCH_LDT = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault())!!

const val PROP_DOWNLOAD_LAST_PAGE_NUMBER = "downloadLastPageNumber"

const val PROP_DOWNLOAD_LAST_PAGE_SIZE = "downloadLastPageSize"

const val PROP_FETCH_NEXT_OFFSET = "fetchNextOffset"

const val DOWNLOAD_PAGE_SIZE = 50

const val FETCH_LIMIT = 100

const val DEV_MAX_OUT_PAGES = 20
const val PRODUCT_MAX_OUT_PAGES = 1000
const val DEV_MAX_PENDING_TASKS = 20
const val PRODUCT_MAX_PENDING_TASKS = 50

val IS_DEVELOPMENT = Files.exists(AppPaths.get("conf/DEVELOPMENT"))

val DOOMSDAY = DateTimes.toLocalDateTime(DateTimes.doomsday)
