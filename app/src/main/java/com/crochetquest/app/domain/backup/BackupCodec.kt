package com.crochetquest.app.domain.backup

import com.crochetquest.app.data.local.AchievementUnlock
import com.crochetquest.app.data.local.CrochetSession
import com.crochetquest.app.data.local.EquippedCosmetic
import com.crochetquest.app.data.local.InventoryItem
import com.crochetquest.app.data.local.PlayerProfile
import com.crochetquest.app.data.local.Project
import com.crochetquest.app.data.local.ProjectUpdate
import com.crochetquest.app.data.local.ProjectYarnLink
import com.crochetquest.app.data.local.QuestCompletion
import com.crochetquest.app.data.local.StashYarn
import com.crochetquest.app.data.local.StitchRecord
import com.crochetquest.app.domain.game.LevelCurve

private typealias Row = Map<String, Any?>

// ---- reading typed values out of a row (JSON gives numbers, CSV gives text) ----

private fun bad(column: String): Nothing =
    throw BackupFormatException("The backup has a missing or unreadable value ('$column').")

private fun Row.str(column: String): String = when (val v = this[column]) {
    null -> ""
    is String -> v
    else -> v.toString()
}

private fun Row.longOrNull(column: String): Long? = when (val v = this[column]) {
    null -> null
    is Long -> v
    is Int -> v.toLong()
    is Double -> v.toLong()
    is String -> v.trim().toLongOrNull() ?: bad(column)
    else -> bad(column)
}

private fun Row.long(column: String): Long = longOrNull(column) ?: bad(column)

private fun Row.intOrNull(column: String): Int? = longOrNull(column)?.also {
    if (it < Int.MIN_VALUE || it > Int.MAX_VALUE) bad(column)
}?.toInt()

private fun Row.int(column: String): Int = intOrNull(column) ?: bad(column)

private fun Row.bool(column: String): Boolean = when (val v = this[column]) {
    is Boolean -> v
    is Long -> v != 0L
    is String -> when (v.trim().lowercase()) {
        "true", "1", "yes" -> true
        "false", "0", "no" -> false
        else -> bad(column)
    }
    else -> bad(column)
}

@Suppress("UNCHECKED_CAST")
private fun asRow(entry: Any?, table: String): Row =
    (entry as? Map<String, Any?>) ?: throw BackupFormatException("The backup is damaged (table '$table').")

/**
 * Reads and writes backups as JSON (full fidelity) or CSV (opens in a spreadsheet).
 * Both formats carry the same tables and restore to exactly the same data.
 */
object BackupCodec {
    const val APP_ID = "crochet-quest"
    const val FORMAT_VERSION = 1
    private const val CSV_MARKER = "#crochet-quest-backup"
    private const val MAX_ROWS_PER_TABLE = 200_000

    /** Table name to its column names, in file order. The order here must match [valuesOf]. */
    private val COLUMNS: Map<String, List<String>> = linkedMapOf(
        "profile" to listOf("id", "catName", "totalXp", "yarnBalance", "createdAt", "lifetimeYarnEarned"),
        "sessions" to listOf("id", "loggedAt", "dayKey", "durationMinutes", "rewarded"),
        "stitches" to listOf("id", "stitchKey", "stitchName", "firstRecordedAt"),
        "projects" to listOf(
            "id", "name", "description", "category", "status", "progressPercent", "targetDateEpochDay",
            "hookSize", "materialNotes", "stitchNotes", "createdAt", "completedAt",
            "completionRewardGranted", "lastRewardedProgress"
        ),
        "projectUpdates" to listOf("id", "projectId", "loggedAt", "fromPercent", "toPercent", "rewarded"),
        "questCompletions" to listOf("id", "questKey", "periodKey", "completedAt", "claimedAt", "associatedProjectId"),
        "achievements" to listOf("id", "achievementKey", "unlockedAt"),
        "inventory" to listOf("id", "itemKey", "purchasedAt"),
        "equipped" to listOf("accessorySlot", "itemKey"),
        "stash" to listOf("id", "name", "colorArgb", "amount", "createdAt"),
        "projectYarn" to listOf("projectId", "yarnId")
    )

    private fun valuesOf(s: BackupSnapshot): Map<String, List<List<Any?>>> = mapOf(
        "profile" to listOfNotNull(s.profile).map {
            listOf(it.id, it.catName, it.totalXp, it.yarnBalance, it.createdAt, it.lifetimeYarnEarned)
        },
        "sessions" to s.sessions.map {
            listOf(it.id, it.loggedAt, it.dayKey, it.durationMinutes, it.rewarded)
        },
        "stitches" to s.stitches.map { listOf(it.id, it.stitchKey, it.stitchName, it.firstRecordedAt) },
        "projects" to s.projects.map {
            listOf(
                it.id, it.name, it.description, it.category, it.status, it.progressPercent,
                it.targetDateEpochDay, it.hookSize, it.materialNotes, it.stitchNotes, it.createdAt,
                it.completedAt, it.completionRewardGranted, it.lastRewardedProgress
            )
        },
        "projectUpdates" to s.projectUpdates.map {
            listOf(it.id, it.projectId, it.loggedAt, it.fromPercent, it.toPercent, it.rewarded)
        },
        "questCompletions" to s.questCompletions.map {
            listOf(it.id, it.questKey, it.periodKey, it.completedAt, it.claimedAt, it.associatedProjectId)
        },
        "achievements" to s.achievements.map { listOf(it.id, it.achievementKey, it.unlockedAt) },
        "inventory" to s.inventory.map { listOf(it.id, it.itemKey, it.purchasedAt) },
        "equipped" to s.equipped.map { listOf(it.accessorySlot, it.itemKey) },
        "stash" to s.stash.map { listOf(it.id, it.name, it.colorArgb, it.amount, it.createdAt) },
        "projectYarn" to s.projectYarn.map { listOf(it.projectId, it.yarnId) }
    )

    private fun build(t: Map<String, List<Row>>): BackupSnapshot {
        fun rows(name: String): List<Row> = t[name].orEmpty().also {
            if (it.size > MAX_ROWS_PER_TABLE) throw BackupFormatException("The backup is too large to read.")
        }
        return BackupSnapshot(
            profile = rows("profile").firstOrNull()?.let {
                PlayerProfile(
                    id = PlayerProfile.SINGLETON_ID,
                    catName = it.str("catName"),
                    totalXp = it.int("totalXp"),
                    yarnBalance = it.int("yarnBalance"),
                    createdAt = it.long("createdAt"),
                    lifetimeYarnEarned = it.intOrNull("lifetimeYarnEarned") ?: 0
                )
            },
            sessions = rows("sessions").map {
                CrochetSession(
                    id = it.long("id"), loggedAt = it.long("loggedAt"), dayKey = it.str("dayKey"),
                    durationMinutes = it.intOrNull("durationMinutes"), rewarded = it.bool("rewarded")
                )
            },
            stitches = rows("stitches").map {
                StitchRecord(
                    id = it.long("id"), stitchKey = it.str("stitchKey"),
                    stitchName = it.str("stitchName"), firstRecordedAt = it.long("firstRecordedAt")
                )
            },
            projects = rows("projects").map {
                Project(
                    id = it.long("id"), name = it.str("name"), description = it.str("description"),
                    category = it.str("category"), status = it.str("status"),
                    progressPercent = it.int("progressPercent"),
                    targetDateEpochDay = it.longOrNull("targetDateEpochDay"),
                    hookSize = it.str("hookSize"), materialNotes = it.str("materialNotes"),
                    stitchNotes = it.str("stitchNotes"), createdAt = it.long("createdAt"),
                    completedAt = it.longOrNull("completedAt"),
                    completionRewardGranted = it.bool("completionRewardGranted"),
                    lastRewardedProgress = it.int("lastRewardedProgress")
                )
            },
            projectUpdates = rows("projectUpdates").map {
                ProjectUpdate(
                    id = it.long("id"), projectId = it.long("projectId"), loggedAt = it.long("loggedAt"),
                    fromPercent = it.int("fromPercent"), toPercent = it.int("toPercent"),
                    rewarded = it.bool("rewarded")
                )
            },
            questCompletions = rows("questCompletions").map {
                QuestCompletion(
                    id = it.long("id"), questKey = it.str("questKey"), periodKey = it.str("periodKey"),
                    completedAt = it.long("completedAt"), claimedAt = it.longOrNull("claimedAt"),
                    associatedProjectId = it.longOrNull("associatedProjectId")
                )
            },
            achievements = rows("achievements").map {
                AchievementUnlock(
                    id = it.long("id"), achievementKey = it.str("achievementKey"),
                    unlockedAt = it.long("unlockedAt")
                )
            },
            inventory = rows("inventory").map {
                InventoryItem(id = it.long("id"), itemKey = it.str("itemKey"), purchasedAt = it.long("purchasedAt"))
            },
            equipped = rows("equipped").map {
                EquippedCosmetic(accessorySlot = it.str("accessorySlot"), itemKey = it.str("itemKey"))
            },
            stash = rows("stash").map {
                StashYarn(
                    id = it.long("id"), name = it.str("name"), colorArgb = it.int("colorArgb"),
                    amount = it.int("amount"), createdAt = it.long("createdAt")
                )
            },
            projectYarn = rows("projectYarn").map {
                ProjectYarnLink(projectId = it.long("projectId"), yarnId = it.long("yarnId"))
            }
        )
    }

    // ------------------------------------------------------------------ writing

    fun encode(snapshot: BackupSnapshot, format: BackupFormat, exportedAt: Long): String =
        when (format) {
            BackupFormat.JSON -> encodeJson(snapshot, exportedAt)
            BackupFormat.CSV -> encodeCsv(snapshot, exportedAt)
        }

    private fun encodeJson(snapshot: BackupSnapshot, exportedAt: Long): String {
        val values = valuesOf(snapshot)
        val tables = LinkedHashMap<String, Any?>()
        for ((name, columns) in COLUMNS) {
            tables[name] = values.getValue(name).map { row -> columns.zip(row).toMap() }
        }
        return MiniJson.write(
            linkedMapOf(
                "app" to APP_ID,
                "formatVersion" to FORMAT_VERSION,
                "exportedAt" to exportedAt,
                "tables" to tables
            )
        )
    }

    private fun encodeCsv(snapshot: BackupSnapshot, exportedAt: Long): String {
        val values = valuesOf(snapshot)
        val out = StringBuilder("\uFEFF") // byte-order mark so spreadsheets read UTF-8 correctly
        out.append(Csv.writeRecord(listOf(CSV_MARKER, FORMAT_VERSION, exportedAt))).append('\n')
        for ((name, columns) in COLUMNS) {
            out.append('\n')
            out.append(Csv.writeRecord(listOf("#table", name))).append('\n')
            out.append(Csv.writeRecord(columns)).append('\n')
            for (row in values.getValue(name)) out.append(Csv.writeRecord(row)).append('\n')
        }
        return out.toString()
    }

    // ------------------------------------------------------------------ reading

    /** Detects JSON or CSV, reads it, and validates it. Throws [BackupFormatException] with a kind message. */
    fun decode(text: String): ParsedBackup {
        val body = text.removePrefix("\uFEFF").trimStart()
        if (body.isEmpty()) throw BackupFormatException("The file is empty.")
        return when {
            body.startsWith("{") -> decodeJson(body)
            body.startsWith(CSV_MARKER) -> decodeCsv(body)
            else -> throw BackupFormatException("This isn't a Crochet Quest backup file.")
        }
    }

    private fun decodeJson(text: String): ParsedBackup {
        val root = MiniJson.parse(text) as? Map<*, *>
            ?: throw BackupFormatException("This isn't a Crochet Quest backup file.")
        if (root["app"] != APP_ID) throw BackupFormatException("This isn't a Crochet Quest backup file.")
        checkVersion((root["formatVersion"] as? Long)?.toInt())
        val exportedAt = root["exportedAt"] as? Long
        val tablesObj = root["tables"] as? Map<*, *>
            ?: throw BackupFormatException("The backup has no data in it.")

        val tables = HashMap<String, List<Row>>()
        for (name in COLUMNS.keys) {
            val list = tablesObj[name] ?: continue
            if (list !is List<*>) throw BackupFormatException("The backup is damaged (table '$name').")
            tables[name] = list.map { entry -> asRow(entry, name) }
        }
        return ParsedBackup(validate(build(tables)), exportedAt, BackupFormat.JSON)
    }

    private fun decodeCsv(text: String): ParsedBackup {
        val records = Csv.parse(text)
        var version: Int? = null
        var exportedAt: Long? = null
        var currentTable: String? = null
        var header: List<String>? = null
        val tables = HashMap<String, MutableList<Row>>()

        for (record in records) {
            if (record.size == 1 && record[0].text.isEmpty() && !record[0].quoted) continue // blank line
            val first = record[0].text
            when {
                first == CSV_MARKER -> {
                    version = record.getOrNull(1)?.text?.trim()?.toIntOrNull()
                    exportedAt = record.getOrNull(2)?.text?.trim()?.toLongOrNull()
                }
                first == "#table" -> {
                    currentTable = record.getOrNull(1)?.text
                    header = null
                }
                currentTable == null -> throw BackupFormatException("The CSV backup is damaged.")
                header == null -> header = record.map { it.text.trim() }
                else -> {
                    val row = HashMap<String, Any?>()
                    header.forEachIndexed { i, column ->
                        val field = record.getOrNull(i)
                        row[column] = if (field == null || (!field.quoted && field.text.isEmpty())) null else field.text
                    }
                    tables.getOrPut(currentTable) { ArrayList() }.add(row)
                }
            }
        }
        checkVersion(version)
        return ParsedBackup(validate(build(tables)), exportedAt, BackupFormat.CSV)
    }

    private fun checkVersion(version: Int?) {
        if (version == null) throw BackupFormatException("This isn't a Crochet Quest backup file.")
        if (version > FORMAT_VERSION) {
            throw BackupFormatException("This backup was made by a newer version of Crochet Quest. Please update the app first.")
        }
        if (version < 1) throw BackupFormatException("This isn't a Crochet Quest backup file.")
    }

    // ------------------------------------------------------------------ validation

    /**
     * Rejects data that would break the game and quietly drops rows that point at things
     * that don't exist (for example a progress entry for a deleted project).
     */
    fun validate(s: BackupSnapshot): BackupSnapshot {
        val profile = s.profile ?: throw BackupFormatException("This backup has no player profile in it.")
        if (profile.totalXp < 0 || profile.yarnBalance < 0 || profile.lifetimeYarnEarned < 0) {
            throw BackupFormatException("The backup has invalid XP or yarn amounts.")
        }
        fun requireUnique(label: String, keys: List<Any?>) {
            if (keys.size != keys.toSet().size) throw BackupFormatException("The backup has duplicate $label.")
        }
        requireUnique("sessions", s.sessions.map { it.id })
        requireUnique("stitches", s.stitches.map { it.id })
        requireUnique("stitches", s.stitches.map { it.stitchKey })
        requireUnique("projects", s.projects.map { it.id })
        requireUnique("progress entries", s.projectUpdates.map { it.id })
        requireUnique("quests", s.questCompletions.map { it.id })
        requireUnique("quests", s.questCompletions.map { it.questKey to it.periodKey })
        requireUnique("achievements", s.achievements.map { it.id })
        requireUnique("achievements", s.achievements.map { it.achievementKey })
        requireUnique("owned items", s.inventory.map { it.id })
        requireUnique("owned items", s.inventory.map { it.itemKey })
        requireUnique("worn items", s.equipped.map { it.accessorySlot })
        requireUnique("yarn colors", s.stash.map { it.id })

        if (s.projects.any { it.progressPercent !in 0..100 || it.lastRewardedProgress !in 0..100 }) {
            throw BackupFormatException("The backup has invalid project progress.")
        }
        if (s.stash.any { it.amount < 0 }) throw BackupFormatException("The backup has invalid yarn amounts.")

        val projectIds = s.projects.map { it.id }.toSet()
        val stashIds = s.stash.map { it.id }.toSet()
        val ownedKeys = s.inventory.map { it.itemKey }.toSet()
        return s.copy(
            profile = profile.copy(id = PlayerProfile.SINGLETON_ID),
            projectUpdates = s.projectUpdates.filter { it.projectId in projectIds },
            projectYarn = s.projectYarn.filter { it.projectId in projectIds && it.yarnId in stashIds }.distinct(),
            equipped = s.equipped.filter { it.itemKey in ownedKeys }
        )
    }

    /** A few friendly lines describing a backup, shown before the player confirms a restore. */
    fun summaryLines(s: BackupSnapshot): List<String> {
        val profile = s.profile ?: return emptyList()
        val level = LevelCurve.levelForXp(profile.totalXp)
        return listOf(
            "${profile.catName}, level $level, ${profile.yarnBalance} yarn balls",
            "${s.projects.size} projects, ${s.sessions.size} sessions, ${s.stitches.size} stitches",
            "${s.achievements.size} achievements, ${s.inventory.size} owned items, ${s.stash.size} yarn colors"
        )
    }
}
