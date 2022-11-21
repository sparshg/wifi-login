@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sparshg.bitslogin

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context.MODE_PRIVATE
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dev.sparshg.bitslogin.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Content()
            }
        }
    }

//    override fun onStop() {
//        super.onStop()
//        Log.e("TAG", "onStop")
//        exitProcess(0)
//    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Content(modifier: Modifier = Modifier) {
//    Log.e("TAG", "CONTENT")
    val openDialog = remember { mutableStateOf(false) }
    val openDialog2 = remember { mutableStateOf(false) }
    val openReview = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dataStore = Store(context)
    val reviewManager = remember {
        ReviewManagerFactory.create(context)
    }
    val reviewInfo = rememberReviewTask(reviewManager)
    val scope = rememberCoroutineScope()
    val pm = context.getSystemService(POWER_SERVICE) as PowerManager
    val isIgnoringBatteryOptimizations = remember {
        mutableStateOf(pm.isIgnoringBatteryOptimizations(context.packageName))
    }
    val prefs = remember {
        context.getSharedPreferences(
            context.getString(R.string.pref_name), MODE_PRIVATE
        )
    }
    val isCredSet = dataStore.credSet.collectAsState(initial = false)
    val isQsAdded = dataStore.qsAdded.collectAsState(initial = false)
    val isServiceRunning = dataStore.service.collectAsState(initial = false)
    val uriHandler = LocalUriHandler.current
    if (isServiceRunning.value) {
        val intent = Intent(context, LoginService::class.java)
        context.startForegroundService(intent)
    }

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                isIgnoringBatteryOptimizations.value =
                    pm.isIgnoringBatteryOptimizations(context.packageName)
            }
            else -> {}
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
    ) {
        if (openDialog.value) {
            var username by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = {
                openDialog.value = false
            },
                icon = { Icon(Icons.Filled.Lock, contentDescription = "Fill credentials") },
                title = {
                    Text("Enter Wi-Fi Credentials")
                },
                text = {
                    Column {
                        OutlinedTextField(value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = modifier.fillMaxWidth()
                        )
                        Spacer(modifier = modifier.height(8.dp))
                        OutlinedTextField(value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        openDialog.value = false
                        if (username.isNotEmpty() && password.isNotEmpty()) {
                            scope.launch {
                                dataStore.setCredSet(true)
                            }
                            VolleySingleton.getInstance(context).cancelAll()
                            prefs.edit().putString("username", username.trim())
                                .putString("password", password.trim()).putBoolean("enabled", false)
                                .apply()
                        }
                    }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        openDialog.value = false
                    }) {
                        Text("Dismiss")
                    }
                })
        }
        if (openReview.value) {
            openReview.value = false
            LaunchedEffect(key1 = reviewInfo) {
                reviewInfo?.let {
                    reviewManager.launchReviewFlow(context as Activity, reviewInfo)
                }
            }
        }
        if (openDialog2.value) {
            var page by remember { mutableStateOf(0) }
            AlertDialog(onDismissRequest = {
                openDialog2.value = false
            }, icon = { Icon(Icons.Filled.Info, contentDescription = "Warning") }, title = {
                Text("How to add Quick Tile")
            }, text = {
                AnimatedContent(targetState = page) { state ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (state) {
                                0 -> "Open notifications panel and go to edit mode."
                                1 -> "Drag the app tile to the top."
                                2 -> "Click when connected to Wi-Fi and wait for it to deactivate."
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = modifier.padding(bottom = 16.dp)
                        )
                        Image(
                            painter = painterResource(
                                id = when (state) {
                                    0 -> R.drawable.screenshot_1
                                    1 -> R.drawable.screenshot_2
                                    2 -> R.drawable.screenshot_3
                                    else -> R.drawable.screenshot_3
                                }
                            ),
                            contentDescription = null,
                            modifier = modifier
                                .clip(RoundedCornerShape(10))
                                .fillMaxWidth()
                        )
                    }


                }
            }, confirmButton = {
                TextButton(onClick = {
                    if (page == 2) {
                        openDialog2.value = false
                    } else {
                        page++
                    }
                }) {
                    Text(if (page == 2) "Close" else "Next")
                }
            }, dismissButton = {
                TextButton(onClick = {
                    if (page != 0) page--
//                        openDialog2.value = false
                }) {
                    Text("Back")
                }
            })
        }
        Column(modifier.fillMaxSize()) {
            LazyColumn(modifier.weight(1f)) {
                item {
                    Text(
                        text = "BITS Wi-Fi Login",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = modifier.padding(start = 20.dp, top = 80.dp, bottom = 24.dp)
                    )
                }
                item {
                    AnimatedContent(targetState = isCredSet.value) {
                        if (!it) {
                            Tile("Wi-Fi Login Credentials",
                                "Set your login credentials. These are only stored on your device.",
                                TileState.CROSS,
                                onClick = {
                                    openDialog.value = true
                                })
                        } else {
                            Tile("Wi-Fi Login Credentials",
                                "Click to update.",
                                TileState.TICKED,
                                onClick = {
                                    openDialog.value = true
                                })
                        }
                    }
                }
                item {
                    AnimatedContent(targetState = isServiceRunning.value) {
                        if (!it) {
                            Tile("Auto-Login Service: Stopped",
                                "Login automatically once connected to Wi-Fi.",
                                TileState.EXCLAMATION,
                                onClick = {
                                    scope.launch {
                                        dataStore.setService(true)
                                    }
                                })
                        } else {
                            Tile("Auto-Login Service: Running",
                                "Hide the service notification.",
                                TileState.TICKED,
                                onClick = {
                                    scope.launch {
                                        dataStore.setService(false)
                                        val intent =
                                            Intent(context, LoginService::class.java)
                                        context.stopService(intent)
                                    }
                                })
                        }
                    }

                }
                item {
                    if (isQsAdded.value) {
                        Tile(
                            "Quick Tile added",
                            "Tap it to login, in-case your device decided to kill the service...",
                            TileState.TICKED,
                        )
                    } else {
                        Tile("Login Quick Tile not added",
                            "Tap it to login, in-case your device decided to kill the service...",
                            TileState.EXCLAMATION,
                            onClick = {
                                openDialog2.value = true
                            })
                    }
                }
                item {
                    if (!isIgnoringBatteryOptimizations.value) {
                        Tile(title = "Disable Battery Optimization",
                            "Auto-Login won't work if app is killed by system.",
                            state = TileState.CROSS,
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Please disable battery optimization for this app.",
                                    Toast.LENGTH_LONG
                                ).show()
                                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            })
                    } else {
                        Tile(title = "Disable Battery Optimization",
                            "Click to follow more device specific steps.",
                            state = TileState.EXCLAMATION,
                            onClick = { uriHandler.openUri("https://dontkillmyapp.com") })
                    }
                }
                item {
                    Text(
                        text = "Tips",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = modifier.padding(start = 20.dp, top = 24.dp, bottom = 24.dp)
                    )
                }

                item {
                    Tile(
                        title = "About your data",
                        desc = "Your credentials remain on your device, IPs of devices using your credentials are listed on the user portal",
                        state = TileState.INFO
                    )
                }
                item {
                    Spacer(modifier = modifier.height(16.dp))
                }
            }
            BottomAppBar(actions = {
                IconButton(onClick = { uriHandler.openUri("https://github.com/sparshg") }) {
                    Image(
                        painter = rememberDrawablePainter(
                            drawable = getDrawable(LocalContext.current, R.drawable.ic_github)
                        ),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                        contentDescription = "GitHub Contact",
                    )
                }
                IconButton(onClick = { uriHandler.openUri("https://www.linkedin.com/in/sparsh-goenka-521ba9222/") }) {
                    Image(
                        painter = rememberDrawablePainter(
                            drawable = getDrawable(LocalContext.current, R.drawable.ic_linkedin),

                            ),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                        contentDescription = "LinkedIn Contact",
                    )
                }
                IconButton(onClick = {
                    openReview.value = true
                }) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Playstore",
                        modifier = modifier.size(28.dp)
                    )
                }
            }, floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:sparshg.contact@gmail.com")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (ex: ActivityNotFoundException) {
//                            Log.e("Error", ex.message.toString())
                        }


                    },
                    containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(
                        1.dp, 0.dp, 1.dp, 1.dp
                    ),
                    icon = { Icon(Icons.Filled.Edit, "Write email") },
                    text = { Text(text = "Feedback") },

                    )
            })

        }
    }
}

@Composable
fun UrlDesc(
    modifier: Modifier = Modifier, desc: String, startIndex: Int, endIndex: Int, url: String
) {
    val annotatedLinkString: AnnotatedString = buildAnnotatedString {
        append(desc)
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onTertiaryContainer
            ), start = 0, end = desc.length
        )
        addStyle(
            style = SpanStyle(
                color = Color(0xff64B5F6),
//                fontSize = 18.sp,
                textDecoration = TextDecoration.Underline
            ), start = startIndex, end = endIndex
        )
        addStringAnnotation(
            tag = "URL", annotation = url, start = startIndex, end = endIndex
        )
    }

// UriHandler parse and opens URI inside AnnotatedString Item in Browse
    val uriHandler = LocalUriHandler.current

// 🔥 Clickable text returns position of text that is clicked in onClick callback
    ClickableText(modifier = modifier,
        text = annotatedLinkString,
        style = MaterialTheme.typography.bodyLarge,
        onClick = {
            annotatedLinkString.getStringAnnotations("URL", it, it).firstOrNull()
                ?.let { stringAnnotation ->
                    uriHandler.openUri(stringAnnotation.item)
                }
        })
}

@Composable
fun rememberReviewTask(reviewManager: ReviewManager): ReviewInfo? {
    var reviewInfo: ReviewInfo? by remember {
        mutableStateOf(null)
    }
    reviewManager.requestReviewFlow().addOnCompleteListener {
        if (it.isSuccessful) {
            reviewInfo = it.result
        }
    }

    return reviewInfo
}

@Composable
fun OnLifecycleEvent(onEvent: (owner: LifecycleOwner, event: Lifecycle.Event) -> Unit) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { owner, event ->
            eventHandler.value(owner, event)
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        Content()
    }
}