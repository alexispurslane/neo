package io.github.alexispurslane.neo.ui.composables.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.alexispurslane.neo.R
import io.github.alexispurslane.neo.autofill
import io.github.alexispurslane.neo.ui.composables.misc.ErrorDialog
import io.github.alexispurslane.neo.viewmodels.LoginViewModel
import kotlinx.coroutines.launch

const val URL_REGEX = "^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"


@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by loginViewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .padding(horizontal = 50.dp)
            .verticalScroll(rememberScrollState())
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Logo(
                Modifier
                    .align(Alignment.CenterHorizontally))

                InstanceUsernamePasswordLoginForm(
                    uiState.instanceApiUrl,
                    uiState.instanceUserName,
                    uiState.instancePassword,
                    uiState.urlValidated,
                    uiState.urlValidationMessage,
                    loginViewModel::onInstanceInfoChange,
                    { loginViewModel.onLogin() },
                )
            }
        }

    if (uiState.isLoginError) {
        ErrorDialog(
            loginViewModel::onLoginErrorDismiss,
            uiState.loginErrorTitle,
            uiState.loginErrorBody
        )
    }
}

@Composable
fun Logo(modifier: Modifier) {
    Row(
        modifier = Modifier.height(100.dp)
    ) {
        val logo1 = ImageVector.vectorResource(id = R.drawable.matrix_logo)
        val painter1 = rememberVectorPainter(image = logo1)
        Image(modifier = modifier
            .aspectRatio(painter1.intrinsicSize.width / painter1.intrinsicSize.height)
            .fillMaxHeight(),
            painter = painter1,
            contentDescription = "Matrix Logo",
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(Color.White)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun InstanceUsernamePasswordLoginForm(
    instanceApiUrl: String,
    instanceUsername: String,
    instancePassword: String,
    urlValidated: Boolean,
    urlValidationMessage: String,
    onInstanceInfoChange: (String, String, String) -> Unit,
    onLogin: () -> Unit,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current
    val modifier = Modifier
        .fillMaxWidth(0.95F)
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusEvent { focusState ->
            if (focusState.isFocused) {
                coroutineScope.launch {
                    bringIntoViewRequester.bringIntoView()
                }
            }
        }

    Text("Log In", fontSize = 30.sp, fontWeight = FontWeight.Black)

    UrlField(
        modifier = modifier,
        value = instanceApiUrl,
        urlValidated = urlValidated,
        urlValidationMessage = urlValidationMessage,
        onValueChange = { onInstanceInfoChange(it, instanceUsername, instancePassword) }
    )

    UsernameField(
        modifier = modifier,
        value = instanceUsername,
        onValueChange = { onInstanceInfoChange(instanceApiUrl, it, instancePassword) }
    )

    PasswordField(
        modifier = modifier,
        value = instancePassword,
        onValueChange = { onInstanceInfoChange(instanceApiUrl, instanceUsername, it) }
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = urlValidated && instanceUsername.isNotBlank() && instancePassword.isNotBlank(),
            onClick = onLogin
        ) {
            Text("Log In")
        }
    }
}

@Composable
fun UrlField(modifier: Modifier, value: String, urlValidated: Boolean, urlValidationMessage: String, onValueChange: (String) -> Unit) {
    val isValidUrl = value.matches(Regex(URL_REGEX))
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("https://<your-matrix-instance>/") },
        supportingText = {
            if (value.isBlank()) {
                Text("The URL to your Matrix instance")
            } else if (!isValidUrl) {
                Text("Please enter a valid URL", color = MaterialTheme.colorScheme.error)
            } else {
                Text(urlValidationMessage, color = if (urlValidated) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.error
                })
            }
        },
        isError = (!isValidUrl && value.isNotEmpty()) || (isValidUrl && !urlValidated && urlValidationMessage.isNotEmpty()),
        singleLine = true,
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.moveFocus(FocusDirection.Down)
            }
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            capitalization = KeyboardCapitalization.None,
            autoCorrect = false,
            imeAction = ImeAction.Next
        ),
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun UsernameField(modifier: Modifier, value: String, onValueChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        modifier = modifier.autofill(
            autofillTypes = listOf(AutofillType.EmailAddress),
            onFill = onValueChange
        ),
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Full Username...") },
        supportingText = {
            if (value.isBlank()) {
                Text("Please enter a valid username", color = MaterialTheme.colorScheme.error)
            }
        },
        isError = value.isBlank(),
        singleLine = true,
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.moveFocus(FocusDirection.Down)
            }
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            capitalization = KeyboardCapitalization.None,
            autoCorrect = true,
            imeAction = ImeAction.Next
        ),
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Username icon"
            )
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PasswordField(modifier: Modifier, value: String, onValueChange: (String) -> Unit) {
    var showPassword by remember { mutableStateOf(false) }
    OutlinedTextField(
        modifier = modifier.autofill(
            autofillTypes = listOf(AutofillType.Password),
            onFill = onValueChange
        ),
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Password...") },
        singleLine = true,
        isError = value.isBlank(),
        supportingText = {
            if (value.isBlank() || value.length < 3) {
                Text("Please enter a valid password", color = MaterialTheme.colorScheme.error)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            capitalization = KeyboardCapitalization.None,
            autoCorrect = false,
            imeAction = ImeAction.Go
        ),
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            PasswordFieldIcon(
                showPassword = showPassword,
                onClick = { showPassword = !showPassword }
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Password icon"
            )
        }
    )
}

@Composable
fun PasswordFieldIcon(showPassword: Boolean, onClick: () -> Unit) {
    val visibilityIcon = ImageVector.vectorResource(id = R.drawable.visibility)
    val visibilityOffIcon = ImageVector.vectorResource(id = R.drawable.visibility_off)

    val (icon, iconColor) = if (showPassword) {
        Pair(visibilityIcon, MaterialTheme.colorScheme.primary)
    } else {
        Pair(visibilityOffIcon, Color.LightGray)
    }

    IconButton(
        modifier = Modifier.size(
            width = 24.dp,
            height = 24.dp
        ),
        onClick = onClick
    ) {
        Icon(
            icon,
            contentDescription = "Visibility",
            tint = iconColor
        )
    }
}
