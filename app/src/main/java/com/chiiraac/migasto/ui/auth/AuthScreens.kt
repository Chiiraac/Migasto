package com.chiiraac.migasto.ui.auth

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chiiraac.migasto.R
import com.chiiraac.migasto.data.AppError
import com.chiiraac.migasto.ui.theme.Palette
import kotlinx.coroutines.launch

/** Logotipo de la app (mismo dibujo que el icono del lanzador). */
@Composable
fun AppLogo(size: Dp = 96.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Brush.linearGradient(listOf(Palette.Indigo, Palette.IndigoDeep))),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun AuthHeader(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        AppLogo()
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AuthLayout(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) { content() }
        }
    }
}

/** Inicio de sesión y registro (modo nube). */
@Composable
fun AuthRoute() {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(application))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    AuthScreen(
        state = state,
        onNameChange = viewModel::setName,
        onEmailChange = viewModel::setEmail,
        onPasswordChange = viewModel::setPassword,
        onToggleMode = viewModel::toggleMode,
        onSubmit = viewModel::submit,
        onForgotPassword = viewModel::sendPasswordReset,
        showGoogle = GoogleSignIn.isConfigured,
        onGoogle = {
            if (viewModel.startGoogle()) {
                scope.launch {
                    // Si la pantalla desaparece (p. ej. al girar el móvil) se trata como cancelado.
                    var token: Result<String> = Result.failure(AppError(AppError.Reason.CANCELLED))
                    try {
                        token = GoogleSignIn.requestIdToken(context)
                    } finally {
                        viewModel.finishGoogle(token)
                    }
                }
            }
        },
    )
}

@Composable
fun AuthScreen(
    state: AuthUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    showGoogle: Boolean = false,
    onGoogle: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    var showPassword by rememberSaveable { mutableStateOf(false) }

    AuthLayout {
        AuthHeader(stringResource(R.string.app_name), stringResource(R.string.auth_tagline))
        Spacer(Modifier.height(12.dp))
        if (showGoogle) {
            GoogleButton(
                loading = state.googleLoading,
                enabled = !state.loading,
                onClick = {
                    focusManager.clearFocus()
                    onGoogle()
                },
            )
            OrDivider(stringResource(R.string.auth_or_email))
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !state.registering,
                onClick = { if (state.registering) onToggleMode() },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) { Text(stringResource(R.string.auth_sign_in), maxLines = 1) }
            SegmentedButton(
                selected = state.registering,
                onClick = { if (!state.registering) onToggleMode() },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) { Text(stringResource(R.string.auth_register), maxLines = 1) }
        }
        if (state.registering) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.auth_name)) },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.large,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.auth_email)) },
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            shape = MaterialTheme.shapes.large,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.auth_password)) },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = stringResource(
                            if (showPassword) R.string.auth_hide_password else R.string.auth_show_password,
                        ),
                    )
                }
            },
            supportingText = if (state.registering) {
                { Text(stringResource(R.string.auth_password_hint)) }
            } else {
                null
            },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onSubmit()
            }),
            shape = MaterialTheme.shapes.large,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        state.resetSentTo?.let {
            Text(
                stringResource(R.string.auth_reset_sent, it),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Button(
            onClick = {
                focusManager.clearFocus()
                onSubmit()
            },
            enabled = state.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
            if (state.loading && !state.googleLoading) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondary)
            } else {
                Text(
                    stringResource(if (state.registering) R.string.auth_register else R.string.auth_sign_in),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        if (!state.registering) {
            TextButton(onClick = onForgotPassword, enabled = !state.loading, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.auth_forgot))
            }
        }
        TextButton(
            onClick = { runCatching { uriHandler.openUri(privacyUrl) } },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                stringResource(R.string.auth_privacy_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Botón "Continuar con Google" con los colores de las pautas de marca de Google. */
@Composable
private fun GoogleButton(loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val container = if (dark) Color(0xFF131314) else Color.White
    val content = if (dark) Color(0xFFE3E3E3) else Color(0xFF1F1F1F)
    val stroke = if (dark) Color(0xFF8E918F) else Color(0xFF747775)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, stroke),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container,
            disabledContentColor = content.copy(alpha = 0.38f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = content)
        } else {
            Image(
                painter = painterResource(R.drawable.ic_google_logo),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.auth_continue_google), style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
private fun OrDivider(text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Bienvenida en modo local: nombre y primer grupo. */
@Composable
fun WelcomeRoute() {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(application))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WelcomeScreen(
        state = state,
        onNameChange = viewModel::setName,
        onGroupNameChange = viewModel::setGroupName,
        onStart = viewModel::startLocal,
    )
}

@Composable
fun WelcomeScreen(
    state: AuthUiState,
    onNameChange: (String) -> Unit,
    onGroupNameChange: (String) -> Unit,
    onStart: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    AuthLayout {
        AuthHeader(stringResource(R.string.welcome_title), stringResource(R.string.welcome_body))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.welcome_name)) },
            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.groupName,
            onValueChange = onGroupNameChange,
            label = { Text(stringResource(R.string.welcome_group)) },
            leadingIcon = { Icon(Icons.Rounded.Group, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onStart()
            }),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Button(
            onClick = {
                focusManager.clearFocus()
                onStart()
            },
            enabled = state.name.isNotBlank() && !state.loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondary)
            } else {
                Text(stringResource(R.string.welcome_start), style = MaterialTheme.typography.titleMedium)
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Smartphone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text(
                stringResource(R.string.welcome_local_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
