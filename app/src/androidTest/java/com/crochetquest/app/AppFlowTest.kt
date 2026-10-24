package com.crochetquest.app

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end flows on a real (emulated) device, against the real database.
 * Each test starts from a freshly reset game, so tests don't depend on each other.
 * Run by the manual "Instrumented tests" GitHub workflow (animations are disabled there,
 * which also exercises the app's reduced-motion path).
 */
@RunWith(AndroidJUnit4::class)
class AppFlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun resetGameAndLaunch() {
        val app = ApplicationProvider.getApplicationContext<CrochetQuestApp>()
        runBlocking { app.container.gameService.resetProgress() }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun closeApp() {
        scenario?.close()
    }

    // ---- helpers ----

    private fun waitForText(text: String, substring: Boolean = false, timeoutMillis: Long = 10_000) {
        compose.waitUntil(timeoutMillis) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun click(text: String) {
        waitForText(text)
        compose.onNodeWithText(text).performClick()
    }

    private fun startGame() {
        click("Start My Quest")
        waitForText("Welcome back, little stitcher!")
    }

    private fun logSession() {
        click("Log Crochet Session")
        waitForText("Log a crochet session")
        compose.onNodeWithText("Log session").performClick()
        waitForText("Lovely stitching!", substring = true)
    }

    // ---- tests ----

    @Test
    fun firstLaunchShowsOnboardingThenHome() {
        waitForText("A little world, one stitch at a time.")
        startGame()
        waitForText("Level 1 · Tiny Beginner")
    }

    @Test
    fun addingAProjectShowsItInTheProjectList() {
        startGame()
        click("Projects")
        waitForText("No projects yet")
        click("Add Your First Project")
        waitForText("New project")
        compose.onNodeWithText("Project name").performTextInput("Test Bunny")
        click("Add project")
        waitForText("Test Bunny")
    }

    @Test
    fun completingAndClaimingAQuestPaysOnce() {
        startGame()
        logSession()
        click("Quests")
        waitForText("Claim reward")
        compose.onNodeWithText("Claim reward").performClick()
        waitForText("Quest claimed", substring = true)
        waitForText("Claimed · Resets daily")
    }

    @Test
    fun buyingAndEquippingAnAccessoryWorks() {
        startGame()
        logSession() // 10 starting + 5 earned = 15 yarn balls = the bell collar
        click("Open Wardrobe")
        waitForText("Buy for 15")
        compose.onNodeWithText("Buy for 15").performClick()
        waitForText("Equip")
        compose.onNodeWithText("Equip").performClick()
        waitForText("Take off")
    }

    @Test
    fun progressSurvivesRecreationAndRestart() {
        click("Biscuit")
        startGame()
        waitForText("Biscuit")
        logSession()
        waitForText("10 / 100 XP to Level 2")

        // configuration change (rotation-like recreation)
        scenario!!.recreate()
        waitForText("Biscuit")
        waitForText("10 / 100 XP to Level 2")

        // full close and reopen of the activity (data comes back from the database)
        scenario!!.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForText("Welcome back, little stitcher!")
        waitForText("Biscuit")
        waitForText("10 / 100 XP to Level 2")
    }
}
