package com.codingwithumair.app.vidcompose.ui.screens.mainScreen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp // FIX: Added missing dp import
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codingwithumair.app.vidcompose.R
import com.codingwithumair.app.vidcompose.model.VideoItem
import com.codingwithumair.app.vidcompose.ui.screens.mainScreenComponents.FolderItemGridLayout
import com.codingwithumair.app.vidcompose.ui.screens.mainScreenComponents.VideoItemGridLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithBottomNavigation(
    onVideoItemClick: (VideoItem) -> Unit
) {
    var bottomNavigationScreen by rememberSaveable {
        mutableStateOf(BottomNavigationScreens.VideosView)
    }

    val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.factory)
    val videosViewStateFlow by mainViewModel.videoItemsStateFlow.collectAsState()
    val foldersViewStateFlow by mainViewModel.folderItemStateFlow.collectAsState()

    // M3 TopBar Scroll Behavior
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // Updated to LargeTopAppBar for the "Anime Rebuild" aesthetic
            LargeTopAppBar(
                title = {
                    Text(
                        text = "AnimeApp", // Replacing with the new app identity
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = bottomNavigationScreen == BottomNavigationScreens.VideosView,
                    label = { Text(text = "Library") },
                    onClick = { bottomNavigationScreen = BottomNavigationScreens.VideosView },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.round_video_library_24),
                            contentDescription = null
                        )
                    }
                )

                NavigationBarItem(
                    selected = bottomNavigationScreen == BottomNavigationScreens.FoldersView,
                    label = { Text(text = "Folders") },
                    onClick = { bottomNavigationScreen = BottomNavigationScreens.FoldersView },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.round_folder_copy_24),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = bottomNavigationScreen,
            label = "ScreenTransition",
            transitionSpec = {
                if (targetState == BottomNavigationScreens.FoldersView) {
                    slideInHorizontally { it } + fadeIn() togetherWith 
                    slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith 
                    slideOutHorizontally { it } + fadeOut()
                }
            }
        ) { navScreen ->
            when (navScreen) {
                BottomNavigationScreens.VideosView -> {
                    VideoItemGridLayout(
                        contentPadding = paddingValues,
                        videoList = videosViewStateFlow,
                        onVideoItemClick = onVideoItemClick,
                    )
                }

                BottomNavigationScreens.FoldersView -> {
                    var foldersVideosNavigation by rememberSaveable {
                        mutableStateOf(FoldersVideosNavigation.FoldersScreen)
                    }

                    Crossfade(
                        targetState = foldersVideosNavigation, 
                        label = "FolderTransition",
                        animationSpec = tween(300, easing = LinearEasing)
                    ) { foldersAndVideosNav ->
                        when (foldersAndVideosNav) {
                            FoldersVideosNavigation.FoldersScreen -> {
                                FolderItemGridLayout(
                                    foldersList = foldersViewStateFlow,
                                    onFolderItemClick = {
                                        mainViewModel.updateCurrentSelectedFolderItem(it)
                                        foldersVideosNavigation = FoldersVideosNavigation.VideosScreen
                                    },
                                    contentPadding = paddingValues
                                )
                            }

                            FoldersVideosNavigation.VideosScreen -> {
                                BackHandler {
                                    foldersVideosNavigation = FoldersVideosNavigation.FoldersScreen
                                }

                                VideoItemGridLayout(
                                    contentPadding = paddingValues,
                                    videoList = mainViewModel.currentSelectedFolder.videoItems,
                                    onVideoItemClick = onVideoItemClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class BottomNavigationScreens {
    VideosView,
    FoldersView
}

private enum class FoldersVideosNavigation {
    FoldersScreen,
    VideosScreen
}
