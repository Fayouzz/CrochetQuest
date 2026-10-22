package com.crochetquest.app

import android.app.Application
import android.content.Context
import com.crochetquest.app.data.local.CrochetDatabase
import com.crochetquest.app.data.repository.GameRepository
import com.crochetquest.app.data.repository.SettingsRepository
import com.crochetquest.app.domain.game.GameService
import com.crochetquest.app.domain.game.SystemGameClock

class CrochetQuestApp : Application() {
    /** Tiny hand-rolled service locator; a DI framework would be overkill here. */
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(context: Context) {
    private val database = CrochetDatabase.create(context)
    val gameRepository = GameRepository(database)
    val gameService = GameService(gameRepository, SystemGameClock)
    val settingsRepository = SettingsRepository(context)
}
