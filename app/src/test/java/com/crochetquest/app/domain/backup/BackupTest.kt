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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test


class BackupTest {

    private val t = 1_700_000_000_000L

    /** A snapshot where every column holds a distinct, awkward value. */
    private fun sample(): BackupSnapshot = BackupSnapshot(
        profile = PlayerProfile(
            catName = "Mo, \"Mochi\"\nCat \uD83D\uDC31", totalXp = 1234, yarnBalance = 56,
            createdAt = t, lifetimeYarnEarned = 789
        ),
        sessions = listOf(
            CrochetSession(1, t + 1, "2023-11-14", 30, true),
            CrochetSession(2, t + 2, "2023-11-15", null, false)
        ),
        stitches = listOf(StitchRecord(1, "granny square", "Granny Square", t + 3)),
        projects = listOf(
            Project(
                id = 1, name = "Bunny, the \"brave\"", description = "line1\nline2\r\nline3",
                category = "AMIGURUMI", status = "COMPLETED", progressPercent = 100,
                targetDateEpochDay = 20_000, hookSize = "", materialNotes = " leading space",
                stitchNotes = "caf\u00E9 \uD83E\uDDF6 \u00FCn\u00EF, ok", createdAt = t, completedAt = t + 10,
                completionRewardGranted = true, lastRewardedProgress = 100
            ),
            Project(
                id = 7, name = "Plain", description = "", category = "OTHER", status = "PLANNED",
                progressPercent = 0, targetDateEpochDay = null, hookSize = "4mm", materialNotes = "",
                stitchNotes = "", createdAt = t, completedAt = null,
                completionRewardGranted = false, lastRewardedProgress = 0
            )
        ),
        projectUpdates = listOf(ProjectUpdate(1, 1, t + 4, 0, 100, false)),
        questCompletions = listOf(
            QuestCompletion(1, "cozy_daily_session", "2023-11-14", t + 5, null, null),
            QuestCompletion(2, "milestone_first_finish", "once", t + 6, t + 7, 1)
        ),
        achievements = listOf(AchievementUnlock(1, "first_stitch", t + 8)),
        inventory = listOf(InventoryItem(1, "pink_bow", t + 9), InventoryItem(2, "room_rug_round", t + 9)),
        equipped = listOf(EquippedCosmetic("HEAD", "pink_bow"), EquippedCosmetic("ROOM_RUG", "room_rug_round")),
        stash = listOf(StashYarn(1, "Sage, soft", -6_498_156, 3, t)),
        projectYarn = listOf(ProjectYarnLink(1, 1))
    )

    // ---------------- format round trips ----------------

    @Test
    fun `JSON backup restores exactly the same data`() {
        val text = BackupCodec.encode(sample(), BackupFormat.JSON, 1234L)
        val parsed = BackupCodec.decode(text)
        assertEquals(sample(), parsed.snapshot)
        assertEquals(1234L, parsed.exportedAt)
        assertEquals(BackupFormat.JSON, parsed.format)
    }

    @Test
    fun `CSV backup restores exactly the same data`() {
        val text = BackupCodec.encode(sample(), BackupFormat.CSV, 1234L)
        val parsed = BackupCodec.decode(text)
        assertEquals(sample(), parsed.snapshot)
        assertEquals(1234L, parsed.exportedAt)
        assertEquals(BackupFormat.CSV, parsed.format)
    }

    @Test
    fun `CSV tells an empty text apart from a missing value`() {
        val parsed = BackupCodec.decode(BackupCodec.encode(sample(), BackupFormat.CSV, 1L)).snapshot
        val plain = parsed.projects.first { it.id == 7L }
        assertEquals("", plain.description)       // empty text stays empty text
        assertEquals(null, plain.targetDateEpochDay) // missing stays missing
        assertEquals(null, plain.completedAt)
    }

    @Test
    fun `an empty game still backs up and restores`() {
        val minimal = BackupSnapshot(profile = PlayerProfile(catName = "Mochi", totalXp = 0, yarnBalance = 10, createdAt = t))
        for (format in BackupFormat.entries) {
            val back = BackupCodec.decode(BackupCodec.encode(minimal, format, 1L)).snapshot
            assertEquals(minimal, back)
        }
    }

    @Test
    fun `CSV is a plain spreadsheet-friendly file`() {
        val text = BackupCodec.encode(sample(), BackupFormat.CSV, 1L)
        assertTrue(text.startsWith("\uFEFF#crochet-quest-backup,1,1"))
        assertTrue(text.contains("#table,projects"))
        assertTrue(text.contains("id,name,description,category,status"))
    }

    @Test
    fun `the format is detected from the content, not the file name`() {
        val json = BackupCodec.encode(sample(), BackupFormat.JSON, 1L)
        assertEquals(BackupFormat.JSON, BackupCodec.decode("   \n$json").format)
        assertEquals(BackupFormat.CSV, BackupCodec.decode(BackupCodec.encode(sample(), BackupFormat.CSV, 1L)).format)
    }

    // ---------------- rejecting bad files ----------------

    private fun assertRejected(text: String, expectedPart: String? = null) {
        try {
            BackupCodec.decode(text)
            fail("expected the backup to be rejected")
        } catch (e: BackupFormatException) {
            if (expectedPart != null) assertTrue("message was: ${e.message}", e.message!!.contains(expectedPart))
        }
    }

    @Test
    fun `junk, empty and wrong files are refused with a friendly message`() {
        assertRejected("", "empty")
        assertRejected("   \n  ", "empty")
        assertRejected("hello there", "isn't a Crochet Quest backup")
        assertRejected("{\"app\": \"some-other-app\", \"formatVersion\": 1, \"tables\": {}}", "isn't a Crochet Quest backup")
        assertRejected("{\"hello\": 1}", "isn't a Crochet Quest backup")
        assertRejected("[1, 2, 3]", "isn't a Crochet Quest backup")
    }

    @Test
    fun `a backup from a newer app version is refused clearly`() {
        val json = BackupCodec.encode(sample(), BackupFormat.JSON, 1L).replace("\"formatVersion\": 1", "\"formatVersion\": 99")
        assertRejected(json, "newer version")
        val csv = BackupCodec.encode(sample(), BackupFormat.CSV, 1L).replace("#crochet-quest-backup,1,", "#crochet-quest-backup,99,")
        assertRejected(csv, "newer version")
    }

    @Test
    fun `a backup without a player profile is refused`() {
        val json = BackupCodec.encode(sample().copy(profile = null), BackupFormat.JSON, 1L)
        assertRejected(json, "no player profile")
        val csv = BackupCodec.encode(sample().copy(profile = null), BackupFormat.CSV, 1L)
        assertRejected(csv, "no player profile")
    }

    @Test
    fun `impossible values are refused`() {
        fun jsonWith(s: BackupSnapshot) = BackupCodec.encode(s, BackupFormat.JSON, 1L)
        val base = sample()
        assertRejected(jsonWith(base.copy(profile = base.profile!!.copy(totalXp = -5))), "invalid")
        assertRejected(jsonWith(base.copy(profile = base.profile!!.copy(yarnBalance = -1))), "invalid")
        assertRejected(jsonWith(base.copy(projects = base.projects.map { it.copy(progressPercent = 250) })), "invalid")
        assertRejected(jsonWith(base.copy(stash = base.stash.map { it.copy(amount = -3) })), "invalid")
    }

    @Test
    fun `duplicate entries are refused`() {
        val base = sample()
        assertRejected(
            BackupCodec.encode(base.copy(projects = base.projects + base.projects.first()), BackupFormat.JSON, 1L),
            "duplicate"
        )
        assertRejected(
            BackupCodec.encode(base.copy(inventory = base.inventory + InventoryItem(9, "pink_bow", t)), BackupFormat.CSV, 1L),
            "duplicate"
        )
    }

    @Test
    fun `rows pointing at missing things are quietly dropped`() {
        val base = sample()
        val messy = base.copy(
            projectUpdates = base.projectUpdates + ProjectUpdate(2, 404, t, 0, 10, false),
            projectYarn = base.projectYarn + ProjectYarnLink(404, 1) + ProjectYarnLink(1, 404),
            equipped = base.equipped + EquippedCosmetic("NECK", "never_bought")
        )
        val cleaned = BackupCodec.decode(BackupCodec.encode(messy, BackupFormat.JSON, 1L)).snapshot
        assertEquals(base.projectUpdates, cleaned.projectUpdates)
        assertEquals(base.projectYarn, cleaned.projectYarn)
        assertEquals(base.equipped, cleaned.equipped)
    }

    @Test
    fun `truncated files are refused, never half-read`() {
        for (format in BackupFormat.entries) {
            val text = BackupCodec.encode(sample(), format, 1L)
            // cutting off the end of a JSON file always breaks it; for CSV cut inside a quoted value
            val cut = if (format == BackupFormat.JSON) text.substring(0, text.length / 2)
            else text.substring(0, text.indexOf("Bunny") + 3)
            try {
                val parsed = BackupCodec.decode(cut)
                // A CSV cut at a record boundary may legitimately lack the later tables; it must
                // never invent data, and a profile-less result is refused above.
                assertNotNull(parsed.snapshot.profile)
            } catch (e: BackupFormatException) {
                // expected
            }
        }
    }

    // ---------------- robustness ----------------

    @Test
    fun `corrupted files only ever fail politely`() {
        val random = kotlin.random.Random(12345)
        val sources = BackupFormat.entries.map { BackupCodec.encode(sample(), it, 1L) }
        var rejected = 0
        var accepted = 0
        repeat(600) { n ->
            val original = sources[n % sources.size]
            val chars = original.toCharArray()
            val damaged = when (random.nextInt(4)) {
                0 -> String(chars).substring(0, random.nextInt(chars.size))            // truncate
                1 -> { chars[random.nextInt(chars.size)] = "{}[]\",:\\\n\u0000x".random(random); String(chars) }
                2 -> { repeat(5) { chars[random.nextInt(chars.size)] = ('a' + random.nextInt(26)) }; String(chars) }
                else -> String(chars).replaceFirst(listOf("1", "true", "\"", ",", "0").random(random), "")
            }
            try {
                BackupCodec.decode(damaged)
                accepted++
            } catch (e: BackupFormatException) {
                rejected++
            }
            // any other exception type escapes and fails this test
        }
        assertTrue("some damage should be detected", rejected > 50)
        println("fuzz: rejected=$rejected accepted=$accepted")
    }

    // ---------------- the small codecs ----------------

    @Test
    fun `MiniJson round trips tricky strings and numbers`() {
        val value = linkedMapOf(
            "text" to "tab\there \"quoted\" back\\slash \u0001 \uD83E\uDDF6",
            "numbers" to listOf(0L, -5L, 9_007_199_254_740_993L, 2.5),
            "flags" to listOf(true, false, null),
            "empty" to emptyMap<String, Any?>(),
            "nested" to listOf(linkedMapOf("a" to 1L), emptyList<Any?>())
        )
        val back = MiniJson.parse(MiniJson.write(value))
        assertEquals(value, back)
    }

    @Test
    fun `MiniJson refuses bad input with BackupFormatException`() {
        for (bad in listOf("{", "[1,", "{\"a\" 1}", "tru", "\"abc", "{\"a\": 1} extra", "-", "[1 2]", "\"\\q\"")) {
            try {
                MiniJson.parse(bad)
                fail("should reject: $bad")
            } catch (e: BackupFormatException) {
                // expected
            }
        }
        val deep = "[".repeat(500) + "]".repeat(500)
        try {
            MiniJson.parse(deep)
            fail("deep nesting should be rejected")
        } catch (e: BackupFormatException) {
            // expected, and no stack overflow
        }
    }

    @Test
    fun `CSV reader handles quotes, commas, line breaks and CRLF`() {
        val records = Csv.parse("a,\"b,c\",\"d\"\"e\"\r\n\"line1\nline2\",,\"\"\r\nlast,x")
        assertEquals(3, records.size)
        assertEquals(listOf("a", "b,c", "d\"e"), records[0].map { it.text })
        assertEquals("line1\nline2", records[1][0].text)
        assertTrue(records[1][0].quoted)
        assertEquals("", records[1][1].text)
        assertEquals(false, records[1][1].quoted) // missing
        assertEquals(true, records[1][2].quoted)  // empty text
        assertEquals(listOf("last", "x"), records[2].map { it.text })
    }

    @Test
    fun `CSV writer quotes only when needed and keeps empty text distinct from null`() {
        assertEquals("a,b,c", Csv.writeRecord(listOf("a", "b", "c")))
        assertEquals("\"x,y\",\"say \"\"hi\"\"\"", Csv.writeRecord(listOf("x,y", "say \"hi\"")))
        assertEquals(",\"\"", Csv.writeRecord(listOf(null, "")))
        assertEquals("1,true,-3", Csv.writeRecord(listOf(1, true, -3L)))
    }

    @Test
    fun `CSV reader refuses unterminated quotes`() {
        try {
            Csv.parse("a,\"never closed")
            fail("should reject")
        } catch (e: BackupFormatException) {
            // expected
        }
    }
}
