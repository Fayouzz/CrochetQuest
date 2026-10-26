package com.crochetquest.app

import android.app.Application
import android.content.Context
import com.crochetquest.app.data.local.CrochetDatabase
import com.crochetquest.app.data.repository.GameRepository
import com.crochetquest.app.data.repository.SettingsRepository
import com.crochetquest.app.domain.game.GameClock
import com.crochetquest.app.domain.game.GameService
import com.crochetquest.app.domain.game.SystemGameClock
import com.crochetquest.app.ui.feedback.SoundPlayer

class CrochetQuestApp : Application() {
    /** Tiny hand-rolled service locator; a DI framework would be overkill here. */
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = CrochetDatabase.create(appContext)

    val gameClock: GameClock = SystemGameClock
    val gameRepository = GameRepository(database)
    val gameService = GameService(gameRepository, gameClock)
    val settingsRepository = SettingsRepository(appContext)

    /** Created the first time a sound is needed, so startup stays fast. */
    val soundPlayer: SoundPlayer by lazy { SoundPlayer(appContext) }
}
