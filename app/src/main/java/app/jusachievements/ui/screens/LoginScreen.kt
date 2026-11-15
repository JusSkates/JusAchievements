package app.jusachievements.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import app.jusachievements.R
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.jusachievements.ui.AppState
import app.jusachievements.ui.theme.JaColors
import kotlinx.coroutines.launch

private const val RA_SETTINGS_URL = "https://retroachievements.org/settings"

@Composable
fun LoginScreen(state: AppState) {
    // Stop landscape keyboards switching to their full-screen "extract" editor, which hides
    // the whole sign-in form behind a blank text box.
    InterceptPlatformTextInput(
        interceptor = { request, next ->
            next.startInputMethod(
                PlatformTextInputMethodRequest { outAttrs ->
                    request.createInputConnection(outAttrs).also {
                        outAttrs.imeOptions = outAttrs.imeOptions or
                            EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
                    }
                }
            )
        },
    ) { LoginContent(state) }
}

@Composable
private fun LoginContent(state: AppState) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(JaColors.Violet.copy(alpha = 0.25f), JaColors.Background),
                    radius = 1400f,
                )
            )
            .safeDrawingPadding()
            .imePadding()
    ) {
        val wide = maxWidth >= 720.dp && maxWidth > maxHeight
        if (wide) {
            Row(Modifier.fillMaxSize().padding(32.dp), verticalAlignment = Alignment.CenterVertically) {
                Brand(Modifier.weight(1f), large = true)
                Box(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) { LoginForm(state) }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Brand(Modifier, large = false)
                Spacer(Modifier.height(24.dp))
                LoginForm(state)
            }
        }
    }
}

@Composable
private fun Brand(modifier: Modifier, large: Boolean) {
    Column(modifier) {
        Image(painterResource(R.drawable.ja_logo), null, Modifier.size(if (large) 96.dp else 72.dp))
        Spacer(Modifier.height(16.dp))
        Text("JusAchievements", style = if (large) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium)
        Text(
            "Your RetroAchievements, at a glance.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoginForm(state: AppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var username by rememberSaveable { mutableStateOf("") }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var showKey by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun submit() {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            error = state.login(username, apiKey)
            busy = false
        }
    }

    Column(
        Modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(JaColors.Surface.copy(alpha = 0.92f))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Sign in", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Use your RetroAchievements username and Web API key.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            leadingIcon = { Icon(Icons.Rounded.Person, null) },
            singleLine = true,
            enabled = !busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("Web API key") },
            leadingIcon = { Icon(Icons.Rounded.Key, null) },
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (showKey) "Hide key" else "Show key")
                }
            },
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            enabled = !busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Button(onClick = ::submit, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = JaColors.Background)
            else Text("Sign in")
        }
        TextButton(
            onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RA_SETTINGS_URL))) }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { Text("Where's my API key? (Settings → Keys)") }
        Text(
            "Your key is encrypted on this device and only ever sent to retroachievements.org.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
