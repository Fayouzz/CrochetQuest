package com.crochetquest.app.ui.screens.onboarding

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crochetquest.app.domain.game.RewardConfig
import com.crochetquest.app.ui.components.PixelSpriteImage
import com.crochetquest.app.ui.components.Sprites

private val NAME_SUGGESTIONS = listOf("Mochi", "Biscuit", "Noodle", "Pudding", "Bean")

@Composable
fun OnboardingScreen(onStart: (String) -> Unit, modifier: Modifier = Modifier) {
    var name by rememberSaveable { mutableStateOf(RewardConfig.DEFAULT_CAT_NAME) }
    val canStart = name.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Crochet Quest",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        PixelSpriteImage(
            sprite = Sprites.cat,
            contentDescription = "A friendly pixel-art cat",
            modifier = Modifier.size(168.dp)
        )
        Text(
            text = "A little world, one stitch at a time.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= RewardConfig.MAX_CAT_NAME_LENGTH) name = it },
            label = { Text("Name your cat") },
            singleLine = true,
            isError = !canStart,
            supportingText = {
                Text(if (canStart) "${name.length}/${RewardConfig.MAX_CAT_NAME_LENGTH}" else "Your cat needs a name")
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (canStart) onStart(name) }),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NAME_SUGGESTIONS.forEach { suggestion ->
                FilterChip(
                    selected = name == suggestion,
                    onClick = { name = suggestion },
                    label = { Text(suggestion) }
                )
            }
        }

        Button(
            onClick = { onStart(name) },
            enabled = canStart,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text("Start My Quest", style = MaterialTheme.typography.titleMedium)
        }
    }
}
