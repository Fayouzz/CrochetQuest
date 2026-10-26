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

/** Everything that makes up a save. Backups and restores move exactly this. */
data class BackupSnapshot(
    val profile: PlayerProfile?,
    val sessions: List<CrochetSession> = emptyList(),
    val stitches: List<StitchRecord> = emptyList(),
    val projects: List<Project> = emptyList(),
    val projectUpdates: List<ProjectUpdate> = emptyList(),
    val questCompletions: List<QuestCompletion> = emptyList(),
    val achievements: List<AchievementUnlock> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),
    val equipped: List<EquippedCosmetic> = emptyList(),
    val stash: List<StashYarn> = emptyList(),
    val projectYarn: List<ProjectYarnLink> = emptyList()
)

/** A friendly, user-facing problem with a backup file. */
class BackupFormatException(message: String) : Exception(message)

/** The result of reading a file: the data plus a little information to show before restoring. */
data class ParsedBackup(
    val snapshot: BackupSnapshot,
    val exportedAt: Long?,
    val format: BackupFormat
)

enum class BackupFormat(val label: String, val extension: String, val mimeType: String) {
    JSON("JSON", "json", "application/json"),
    CSV("CSV", "csv", "text/csv")
}
