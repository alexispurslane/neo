package io.github.alexispurslane.bloc.ui.composables.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import io.github.alexispurslane.bloc.R
import kotlinx.coroutines.launch
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.alexispurslane.bloc.ui.models.LoginViewModel
import io.github.alexispurslane.bloc.autofill
import io.github.alexispurslane.bloc.ui.theme.EngineeringOrange

const val URL_REGEX = "^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
const val EMAIL_REGEX = "(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"


@Composable
fun LoginScreen(
    setLoggedIn: (Boolean) -> Unit,
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by loginViewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .padding(horizontal = 50.dp)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RevoltLogo(
                Modifier
                    .align(Alignment.CenterHorizontally))

            if (!uiState.mfa) {
                InstanceEmailPasswordLoginForm(
                    uiState.instanceApiUrl,
                    uiState.instanceEmailAddress,
                    uiState.instancePassword,
                    uiState.urlValidated,
                    uiState.urlValidationMessage,
                    loginViewModel::onInstanceInfoChange,
                    { loginViewModel.onLogin(setLoggedIn) },
                    loginViewModel::testApiUrl,
                )
            } else {
                MultiFactorLoginForm(
                    uiState.mfaAllowedMethods,
                    loginViewModel::onBack,
                    { a, b -> (loginViewModel::onMultiFactorLoginConfirm)(a, b, setLoggedIn) }
                )
            }
        }
    }

    if (uiState.isLoginError) {
        LoginErrorDialog(
            loginViewModel::onLoginErrorDismiss,
            uiState.loginErrorTitle,
            uiState.loginErrorBody
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginErrorDialog(
    onLoginErrorDismiss: () -> Unit,
    loginErrorTitle: String,
    loginErrorBody: String
) {
    AlertDialog(onDismissRequest = onLoginErrorDismiss) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Icon(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                        .size(100.dp)
                        .padding(vertical = 24.dp),
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Login Error Dialog"
                )
                Text(loginErrorTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = loginErrorBody,
                )
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(
                    onClick = onLoginErrorDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Groovy!")
                }
            }
        }
    }
}

@Composable
fun RevoltLogo(modifier: Modifier) {
    val revoltLogo = ImageVector.vectorResource(id = R.drawable.revolt_logo)
    val painter = rememberVectorPainter(image = revoltLogo)

    Image(modifier = modifier
        .aspectRatio(painter.intrinsicSize.width / painter.intrinsicSize.height)
        .fillMaxWidth(),
        painter = painter,
        contentDescription = "Revolt Logo",
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(EngineeringOrange)
    )
}

@Composable
fun MultiFactorLoginForm(
    mfaAllowedMethods: List<String>,
    onBack: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    var mfaMethod by remember { mutableStateOf(0) }
    var mfaResponse by remember { mutableStateOf("") }

    Text("Multi-Factor Authentication", fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 40.sp)

    TabRow(
        modifier = Modifier.fillMaxWidth(),
        selectedTabIndex = mfaMethod,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        mfaAllowedMethods.forEachIndexed { index, mfaAllowedMethod ->
            Tab(
                selected = mfaMethod == index,
                onClick = { mfaMethod = index },
                text = { Text(mfaAllowedMethod) }
            )
        }
    }

    when (mfaAllowedMethods[mfaMethod]) {
        "Recovery" -> OutlinedTextField(
            value = mfaResponse,
            onValueChange = { mfaResponse = it },
            singleLine = true,
            placeholder = { Text("Recovery code...") },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Warning, contentDescription = "Recovery Code")
            }
        )
        "Password" -> OutlinedTextField(
            value = mfaResponse,
            onValueChange = { mfaResponse = it },
            singleLine = true,
            placeholder = { Text("MFA password...") },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Warning, contentDescription = "MFA Password")
            }
        )
        "Totp" -> OutlinedTextField(
            value = mfaResponse,
            onValueChange = { mfaResponse = it },
            singleLine = true,
            placeholder = { Text("One-time passcode...") },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Lock, contentDescription = "One-Time Passcode")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword
            )
        )
        else -> return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = mfaResponse.isNotBlank(),
            onClick = { onLogin(mfaAllowedMethods[mfaMethod], mfaResponse) }
        ) {
            Text("Log In")
        }

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            onClick = { onBack() }
        ) {
            Text("Back")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun InstanceEmailPasswordLoginForm(
    instanceApiUrl: String,
    instanceEmailAddress: String,
    instancePassword: String,
    urlValidated: Boolean,
    urlValidationMessage: String,
    onInstanceInfoChange: (String, String, String) -> Unit,
    onLogin: () -> Unit,
    testApiUrl: () -> Unit,
) {
    val isValidUrl = instanceApiUrl.matches(Regex(URL_REGEX))
    val isValidEmail = instanceEmailAddress.matches(Regex(EMAIL_REGEX))

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
        onValueChange = { onInstanceInfoChange(it, instanceEmailAddress, instancePassword) }
    )

    EmailAddressField(
        modifier = modifier,
        value = instanceEmailAddress,
        onValueChange = { onInstanceInfoChange(instanceApiUrl, it, instancePassword) }
    )

    PasswordField(
        modifier = modifier,
        value = instancePassword,
        onValueChange = { onInstanceInfoChange(instanceApiUrl, instanceEmailAddress, it) }
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = isValidUrl && isValidEmail && instancePassword.isNotBlank(),
            onClick = { if (isValidUrl && isValidEmail && instancePassword.isNotBlank()) onLogin() }
        ) {
            Text("Log In")
        }

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = isValidUrl,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
            onClick = { if (isValidUrl) testApiUrl() }
        ) {
            Text("Test API URL")
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
        placeholder = { Text("https://<your-revolt-instance>/api") },
        supportingText = {
            if (value.isBlank()) {
                Text("The URL to your Revolt instance's API")
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
fun EmailAddressField(modifier: Modifier, value: String, onValueChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val isValidEmail = value.matches(Regex(EMAIL_REGEX))

    OutlinedTextField(
        modifier = modifier.autofill(
            autofillTypes = listOf(AutofillType.EmailAddress),
            onFill = onValueChange
        ),
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Email address...") },
        supportingText = {
            if (!isValidEmail && value.isNotBlank()) {
                Text("Please enter a valid email address", color = MaterialTheme.colorScheme.error)
            }
        },
        isError = !isValidEmail && value.isNotEmpty(),
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
                imageVector = Icons.Filled.Email,
                contentDescription = "Email address icon"
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
