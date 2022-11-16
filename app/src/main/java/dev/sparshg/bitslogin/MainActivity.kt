@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sparshg.bitslogin

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getDrawable
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dev.sparshg.bitslogin.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch


data class UserPreferences(val showCompleted: Boolean)

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
fun Content() {
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
        val intent = Intent(context, MyForegroundService::class.java)
        context.startForegroundService(intent)
    }

    Surface(
        modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
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
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth()
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
                            prefs.edit().putString("username", username)
                                .putString("password", password).apply()
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
                        modifier = Modifier.padding(bottom = 16.dp)
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
                        modifier = Modifier
                            .clip(RoundedCornerShape(10))
                            .fillMaxWidth()
                    )}


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
        Column(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1f)) {
                item {
                    Text(
                        text = "BITS Wi-Fi Login",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 20.dp, top = 80.dp, bottom = 24.dp)
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
                                "Your login credentials are set. Click to update",
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
                                "Login automatically once connected to Wi-Fi without any user interaction",
                                TileState.EXCLAMATION,
                                onClick = {
                                    scope.launch {
                                        dataStore.setService(true)
                                    }
                                })
                        } else {
                            Tile("Auto-Login Service: Running",
                                "Hide the service notification by long pressing it. Use quick tile to one-tap login in case auto-login fails",
                                TileState.TICKED,
                                onClick = {
                                    scope.launch {
                                        dataStore.setService(false)
                                        val intent =
                                            Intent(context, MyForegroundService::class.java)
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
                            "Tap the quick tile to login without further interaction in case auto-login service is off/fails",
                            TileState.TICKED,
                        )
                    } else {
                        Tile("Login Quick Tile not added",
                            "Tap the quick tile to login without further interaction in case auto-login service is off/fails",
                            TileState.EXCLAMATION,
                            onClick = {
                                openDialog2.value = true
                            })
                    }
                }
                item {
                    Text(
                        text = "Tips",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 24.dp)
                    )
                }
                item {
                    Tile(title = "About service", desc = "You can stop the service from running in background when outside campus to save resources", state = TileState.INFO)
                }
                item {
                    Tile(title = "About your data", desc = "Your credentials remain on your device, you can check IPs of devices that connected using your credentials from the user portal", state = TileState.INFO)
                }
                item {
                    Tile(title = "Rate the app", desc = "Rate the app on play store if you liked it", state = TileState.INFO)
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
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
                    Icon(Icons.Filled.Star, contentDescription = "Playstore", modifier = Modifier.size(28.dp))
                }
            }, floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, "sparshg.contact@gmail.com")
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

//            Row(
//                Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 16.dp, horizontal = 24.dp)
//            ) {
//                ContactButton(title = "LINKEDIN", icon = Icons.Filled.MailOutline, uri = "https://google.com")
//                Spacer(modifier = Modifier.width(8.dp))
//                ContactButton(title = "GITHUB", icon = Icons.Filled.MailOutline, uri = "https://google.com")
//                Spacer(modifier = Modifier.width(8.dp))
//                ContactButton(title = "MAIL", icon = Icons.Filled.MailOutline, uri = "https://google.com")
//            }

        }
    }
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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        Content()
    }
}