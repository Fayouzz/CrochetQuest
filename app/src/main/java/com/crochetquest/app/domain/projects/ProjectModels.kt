package com.crochetquest.app.domain.projects

import com.crochetquest.app.data.local.Project

enum class ProjectStatus(val label: String) {
    PLANNED("Planned"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed");

    companion object {
        fun fromName(name: String): ProjectStatus = entries.firstOrNull { it.name == name } ?: PLANNED
    }
}

enum class ProjectCategory(val label: String) {
    AMIGURUMI("Amigurumi and plushies"),
    CLOTHING("Clothing"),
    ACCESSORIES("Accessories"),
    HOME_DECOR("Home decor"),
    FLOWERS("Flowers and plants"),
    GIFTS("Gifts"),
    TINY("Tiny creations"),
    OTHER("Other");

    companion object {
        fun fromName(name: String): ProjectCategory = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

val Project.statusEnum: ProjectStatus get() = ProjectStatus.fromName(status)
val Project.categoryEnum: ProjectCategory get() = ProjectCategory.fromName(category)

/**
 * What the editor sends to the game service. Only [name] is required.
 * [status] is only used when creating (Planned or In progress); afterwards status changes go
 * through start / finish / reopen so rewards can never be granted by editing a form.
 */
data class ProjectDraft(
    val id: Long? = null,
    val name: String,
    val description: String = "",
    val category: ProjectCategory = ProjectCategory.OTHER,
    val status: ProjectStatus = ProjectStatus.PLANNED,
    val targetDateEpochDay: Long? = null,
    val hookSize: String = "",
    val materialNotes: String = "",
    val stitchNotes: String = ""
)
