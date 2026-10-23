package com.crochetquest.app.domain.game

import com.crochetquest.app.domain.projects.ProjectCategory
import com.crochetquest.app.domain.projects.ProjectDraft
import com.crochetquest.app.domain.projects.ProjectStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProjectRulesTest {
    private lateinit var store: FakeGameStore
    private lateinit var clock: TestClock
    private lateinit var service: GameService

    @Before
    fun setUp() {
        store = FakeGameStore()
        clock = TestClock()
        service = GameService(store, clock)
    }

    private suspend fun newProject(
        name: String = "Bunny",
        category: ProjectCategory = ProjectCategory.AMIGURUMI,
        status: ProjectStatus = ProjectStatus.PLANNED
    ): Long {
        service.createProfile("Mochi")
        return service.saveProject(ProjectDraft(name = name, category = category, status = status))!!.projectId
    }

    private val xp get() = store.profile!!.totalXp
    private val yarn get() = store.profile!!.yarnBalance

    // ---- creating and editing ----

    @Test
    fun `only the project name is required`() = runTest {
        service.createProfile("Mochi")
        assertNull(service.saveProject(ProjectDraft(name = "   ")))
        val id = service.saveProject(ProjectDraft(name = "Scarf"))!!.projectId
        val project = store.projects.getValue(id)
        assertEquals("PLANNED", project.status)
        assertEquals(0, project.progressPercent)
        assertEquals(clock.nowMillis(), project.createdAt)
    }

    @Test
    fun `creating a project earns no xp or yarn directly`() = runTest {
        service.createProfile("Mochi")
        val result = service.saveProject(ProjectDraft(name = "Scarf"))!!
        assertEquals(0, result.xpEarned)
        assertEquals(0, xp)
    }

    @Test
    fun `new projects can start in progress but never completed`() = runTest {
        service.createProfile("Mochi")
        val a = service.saveProject(ProjectDraft(name = "A", status = ProjectStatus.IN_PROGRESS))!!.projectId
        val b = service.saveProject(ProjectDraft(name = "B", status = ProjectStatus.COMPLETED))!!.projectId
        assertEquals("IN_PROGRESS", store.projects.getValue(a).status)
        assertEquals("IN_PROGRESS", store.projects.getValue(b).status)
        assertFalse(store.projects.getValue(b).completionRewardGranted)
    }

    @Test
    fun `editing details changes neither status, progress nor rewards`() = runTest {
        val id = newProject()
        service.setProjectProgress(id, 40)
        val xpBefore = xp
        val before = store.projects.getValue(id)

        service.saveProject(
            ProjectDraft(id = id, name = "Big Bunny", category = ProjectCategory.GIFTS, hookSize = "4mm",
                materialNotes = "cream + pink", status = ProjectStatus.COMPLETED)
        )

        val after = store.projects.getValue(id)
        assertEquals("Big Bunny", after.name)
        assertEquals("GIFTS", after.category)
        assertEquals(before.status, after.status)
        assertEquals(before.progressPercent, after.progressPercent)
        assertFalse(after.completionRewardGranted)
        assertEquals(xpBefore, xp)
    }

    @Test
    fun `editing an unknown project returns null`() = runTest {
        service.createProfile("Mochi")
        assertNull(service.saveProject(ProjectDraft(id = 999, name = "Ghost")))
    }

    // ---- progress and the meaningful-update rule ----

    @Test
    fun `a big enough progress step earns the update reward`() = runTest {
        val id = newProject()
        val xpBefore = xp
        val result = service.setProjectProgress(id, 25)!!
        assertTrue(result.rewardedUpdate)
        assertEquals(xpBefore + RewardConfig.PROJECT_UPDATE.xp, xp)
        assertEquals(25, store.projects.getValue(id).lastRewardedProgress)
    }

    @Test
    fun `a tiny step is saved but not rewarded`() = runTest {
        val id = newProject()
        val result = service.setProjectProgress(id, 5)!!
        assertFalse(result.rewardedUpdate)
        assertEquals(0, xp)
        assertEquals(5, store.projects.getValue(id).progressPercent)
    }

    @Test
    fun `sliding back and forth cannot farm rewards`() = runTest {
        val id = newProject()
        service.setProjectProgress(id, 50)
        val xpAfterFirst = xp
        repeat(5) {
            service.setProjectProgress(id, 10)
            service.setProjectProgress(id, 50)
        }
        assertEquals(xpAfterFirst, xp)
    }

    @Test
    fun `progress past the previous best is rewarded again`() = runTest {
        val id = newProject()
        service.setProjectProgress(id, 50)
        val xpBefore = xp
        assertTrue(service.setProjectProgress(id, 60)!!.rewardedUpdate)
        assertEquals(xpBefore + RewardConfig.PROJECT_UPDATE.xp, xp)
    }

    @Test
    fun `moving a planned project forward makes it in progress`() = runTest {
        val id = newProject()
        service.setProjectProgress(id, 5)
        assertEquals("IN_PROGRESS", store.projects.getValue(id).status)
    }

    @Test
    fun `progress is clamped and unchanged values are no-ops`() = runTest {
        val id = newProject()
        service.setProjectProgress(id, 250)
        assertEquals(100, store.projects.getValue(id).progressPercent)
        val updates = store.projectUpdates.size
        service.setProjectProgress(id, 100)
        assertEquals(updates, store.projectUpdates.size)
    }

    @Test
    fun `only forward moves are written to the history`() = runTest {
        val id = newProject()
        service.setProjectProgress(id, 30)
        service.setProjectProgress(id, 20)
        assertEquals(1, store.projectUpdates.size)
        assertEquals(0, store.projectUpdates.single().fromPercent)
        assertEquals(30, store.projectUpdates.single().toPercent)
    }

    @Test
    fun `completed projects reject progress changes until reopened`() = runTest {
        val id = newProject()
        service.completeProject(id)
        assertNull(service.setProjectProgress(id, 10))
        assertEquals(100, store.projects.getValue(id).progressPercent)
    }

    // ---- completion rewards ----

    @Test
    fun `finishing a project grants the completion reward`() = runTest {
        val id = newProject()
        val xpBefore = xp
        val yarnBefore = yarn
        val result = service.completeProject(id)!!
        assertTrue(result.completedNow)
        assertEquals(xpBefore + RewardConfig.PROJECT_COMPLETE.xp, xp)
        assertEquals(yarnBefore + RewardConfig.PROJECT_COMPLETE.yarn, yarn)
        val project = store.projects.getValue(id)
        assertEquals("COMPLETED", project.status)
        assertEquals(100, project.progressPercent)
        assertNotNull(project.completedAt)
        assertTrue(project.completionRewardGranted)
    }

    @Test
    fun `finishing twice in a row does nothing the second time`() = runTest {
        val id = newProject()
        service.completeProject(id)
        val xpAfter = xp
        assertNull(service.completeProject(id))
        assertEquals(xpAfter, xp)
    }

    @Test
    fun `reopening and finishing again never pays twice`() = runTest {
        val id = newProject()
        service.completeProject(id)
        val xpAfter = xp
        val yarnAfter = yarn

        assertTrue(service.reopenProject(id))
        val reopened = store.projects.getValue(id)
        assertEquals("IN_PROGRESS", reopened.status)
        assertNull(reopened.completedAt)
        assertTrue(reopened.completionRewardGranted)
        assertEquals(xpAfter, xp)

        val second = service.completeProject(id)!!
        assertEquals(0, second.xpEarned)
        assertEquals(0, second.yarnEarned)
        assertEquals(xpAfter, xp)
        assertEquals(yarnAfter, yarn)
    }

    @Test
    fun `reopening only works on completed projects`() = runTest {
        val id = newProject()
        assertFalse(service.reopenProject(id))
    }

    @Test
    fun `finishing from partial progress does not also pay an update reward`() = runTest {
        val id = newProject()
        service.setProjectProgress(id, 5)
        val xpBefore = xp
        val result = service.completeProject(id)!!
        assertFalse(result.rewardedUpdate)
        assertEquals(xpBefore + RewardConfig.PROJECT_COMPLETE.xp, xp)
    }

    @Test
    fun `starting a planned project is free and one-way`() = runTest {
        val id = newProject()
        assertTrue(service.startProject(id))
        assertFalse(service.startProject(id))
        assertEquals("IN_PROGRESS", store.projects.getValue(id).status)
        assertEquals(0, xp)
    }

    @Test
    fun `deleting a project removes it and its history`() = runTest {
        val id = newProject()
        service.setProjectProgress(id, 30)
        assertTrue(service.deleteProject(id))
        assertNull(store.projects[id])
        assertTrue(store.projectUpdates.isEmpty())
        assertFalse(service.deleteProject(id))
    }

    @Test
    fun `project operations without a profile are rejected`() = runTest {
        assertNull(service.saveProject(ProjectDraft(name = "X")))
        assertNull(service.setProjectProgress(1, 10))
        assertNull(service.completeProject(1))
    }
}
