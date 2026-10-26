package com.crochetquest.app.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crochetquest.app.domain.stats.GentleStats
import com.crochetquest.app.domain.stats.YearRecap
import com.crochetquest.app.domain.stats.formatMinutes
import com.crochetquest.app.ui.GameUiState
import com.crochetquest.app.ui.components.CozyCard
import com.crochetquest.app.ui.components.PixelSpriteImage
import com.crochetquest.app.ui.components.Sprites
import java.time.format.TextStyle
import java.util.Locale

/** Gentle totals and the "Year in yarn" recap. Nothing here is a goal or a streak. */
@Composable
fun StatsScreen(state: GameUiState.Ready, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val stats = state.stats
    var selectedYear by rememberSaveable { mutableStateOf(stats.years.firstOrNull() ?: 0) }
    val year = if (selectedYear in stats.years) selectedYear else stats.years.first()
    val recap = stats.recaps.getValue(year)

    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Your stats", style = MaterialTheme.typography.titleLarge)
            }
        }
        item(key = "gentle") { GentleStatsCard(stats.gentle) }
        item(key = "recap-title") {
            Column {
                Text("Your year in yarn", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "A look back at what your hands made. No targets, just memories.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (stats.years.size > 1) {
            item(key = "years") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.years.forEach { y ->
                        FilterChip(
                            selected = y == year,
                            onClick = { selectedYear = y },
                            label = { Text(y.toString()) }
                        )
                    }
                }
            }
        }
        item(key = "recap") { RecapCard(recap, state.catName) }
    }
}

@Composable
private fun GentleStatsCard(stats: GentleStats) {
    CozyCard(modifier = Modifier.fillMaxWidth()) {
        StatLine("Time crocheted", formatMinutes(stats.totalMinutes))
        StatLine("Sessions logged", stats.totalSessions.toString())
        StatLine("Projects finished this year", stats.projectsFinishedThisYear.toString())
        StatLine("Projects finished, all time", stats.projectsFinishedAllTime.toString())
        StatLine(
            "Yarn stash",
            "${stats.stashColors} ${if (stats.stashColors == 1) "color" else "colors"}, " +
                "${stats.stashSkeins} ${if (stats.stashSkeins == 1) "skein" else "skeins"}"
        )
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun RecapCard(recap: YearRecap, catName: String) {
    CozyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PixelSpriteImage(Sprites.yarnBall, Modifier.size(48.dp))
        Spacer(Modifier.height(8.dp))
        Text("${recap.year} in yarn", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        if (recap.isEmpty) {
            Text(
                "A blank page: no crochet logged in ${recap.year} yet. $catName is ready whenever you are.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            return@CozyCard
        }

        val lines = buildList {
            if (recap.sessionCount > 0) {
                val days = "${recap.activeDays} ${if (recap.activeDays == 1) "day" else "days"}"
                add(
                    if (recap.totalMinutes > 0) {
                        "You crocheted for ${formatMinutes(recap.totalMinutes)} across " +
                            "${recap.sessionCount} ${if (recap.sessionCount == 1) "session" else "sessions"} on $days."
                    } else {
                        "You logged ${recap.sessionCount} ${if (recap.sessionCount == 1) "session" else "sessions"} on $days."
                    }
                )
            }
            if (recap.longestSessionMinutes > 0) {
                add("Your longest stretch was ${formatMinutes(recap.longestSessionMinutes)}.")
            }
            recap.busiestMonth?.let {
                add("${it.getDisplayName(TextStyle.FULL, Locale.getDefault())} was your busiest month.")
            }
            if (recap.projectsFinished > 0) {
                add(
                    "You finished ${recap.projectsFinished} ${if (recap.projectsFinished == 1) "project" else "projects"}" +
                        (recap.topCategory?.let { ", most of them in ${it.label.lowercase()}" } ?: "") + "."
                )
            }
            if (recap.stitchesDiscovered > 0) {
                add("You discovered ${recap.stitchesDiscovered} new ${if (recap.stitchesDiscovered == 1) "stitch" else "stitches"}.")
            }
            if (recap.achievementsUnlocked > 0) {
                add("You unlocked ${recap.achievementsUnlocked} ${if (recap.achievementsUnlocked == 1) "achievement" else "achievements"}.")
            }
        }
        lines.forEach {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Thank you for the cozy year, from $catName.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
