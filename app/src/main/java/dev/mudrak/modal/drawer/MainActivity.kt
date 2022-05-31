package dev.mudrak.modal.drawer

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.mudrak.modal.drawer.ui.theme.ModalburgerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val drawerState: DrawerState2 = rememberDrawerState2(DrawerValue.Closed)
            val isOpen = remember {
                mutableStateOf(false)
            }

            val scope = rememberCoroutineScope()

            ModalburgerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(Modifier.fillMaxSize()) {
                        TopBar {

                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                        ) {

                        }

                        BottomAppBar(Modifier.fillMaxWidth()) {
                            Text(
                                text = "Tab1",
                                modifier = Modifier.clickable { isOpen.value = true }
                            )
                            Text(
                                text = "Tab2",
                                modifier = Modifier.clickable { isOpen.value = true }
                            )
                            Text(
                                text = "Tab3",
                                modifier = Modifier.clickable { isOpen.value = true }
                            )
                        }
                    }

                    if (isOpen.value) {
                        FloatingDrawer(
                            drawerState = drawerState,
                            onOpen = {
                                scope.launch {
                                    drawerState.open()
                                }
                            },
                            onClose = {
                                scope.launch {
                                    drawerState.close()
                                    isOpen.value = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String = "Sample title",
    buttonIcon: ImageVector = Icons.Filled.Menu,
    onButtonClicked: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title
            )
        },
        navigationIcon = {
            IconButton(onClick = { onButtonClicked() }) {
                Icon(buttonIcon, contentDescription = "")
            }
        },
        backgroundColor = MaterialTheme.colors.primaryVariant
    )
}

@Composable
private fun FloatingDrawer(
    drawerState: DrawerState2 = rememberDrawerState2(DrawerValue.Closed),
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    FullscreenPopup {
        ModalDrawer2(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                DrawerMenu(
                    onDestinationClicked = {
                        scope.launch {
                            onClose()
                        }
                    }
                )
            },
            onClicked = {
                scope.launch {  onClose() }
            },
        ) {
            DisposableEffect(drawerState) {
                onOpen()

                onDispose {
                    Log.e("afds", "onDispose")
                }
            }
        }
    }
}

@Composable
private fun DrawerMenu(
    modifier: Modifier = Modifier,
    onDestinationClicked: (route: String) -> Unit
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(start = 24.dp, top = 48.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = "App icon"
        )
        listOf("a", "b", "c").forEach { screen ->
            Spacer(Modifier.height(24.dp))
            Text(
                text = screen,
                style = MaterialTheme.typography.h4,
                modifier = Modifier.clickable {
                    onDestinationClicked(screen)
                }
            )
        }
    }
}