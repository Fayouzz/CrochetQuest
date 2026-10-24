package com.crochetquest.app.ui.util

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True only when the in-app animation toggle is on AND Android's system "remove animations"
 * (animator duration scale) is not set to zero.
 */
@Composable
fun rememberAnimationsAllowed(userEnabled: Boolean): Boolean {
    val context = LocalContext.current
    val systemScale = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }
    return userEnabled && systemScale > 0f
}
