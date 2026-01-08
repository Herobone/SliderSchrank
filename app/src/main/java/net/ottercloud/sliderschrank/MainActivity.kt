/*
 * Copyright (c) 2025 OtterCloud
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.ottercloud.sliderschrank

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import net.ottercloud.sliderschrank.ui.PieceEditScreen
import net.ottercloud.sliderschrank.ui.theme.SliderSchrankTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SliderSchrankTheme {
                SliderSchrankApp(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
private fun SliderSchrankApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            AppDestinations.entries.filter { it.navigatorVisible }.forEach { destination ->
                val selected =
                    currentDestination?.hierarchy?.any {
                        val route = it.route?.substringBefore("?")
                        route == destination.name
                    } == true
                item(
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.labelRes),
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    label = null,
                    selected = selected,
                    onClick = {
                        navController.navigate(destination.name) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // re-selecting the same item
                            launchSingleTop = true
                            // Restore state when re-selecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            val contentModifier = Modifier.padding(innerPadding)
            NavHost(
                navController = navController,
                startDestination = AppDestinations.HOME.name,
                modifier = contentModifier
            ) {
                composable(
                    route = "${AppDestinations.HOME.name}?outfitId={outfitId}",
                    arguments = listOf(
                        navArgument("outfitId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
                    )
                ) { backStackEntry ->
                    val outfitId = backStackEntry.arguments?.getLong("outfitId") ?: -1L
                    Log.d("MainActivity", "HOME composable: outfitId=$outfitId")
                    HomeScreen(
                        modifier = Modifier.fillMaxSize(),
                        loadOutfitId = if (outfitId > 0) outfitId else null
                    )
                }
                composable(AppDestinations.CAMERA.name) {
                    CameraScreen(
                        modifier = Modifier.fillMaxSize(),
                        onImageSave = { uri ->
                            val encodedUri = android.net.Uri.encode(uri.toString())
                            navController.navigate(
                                "${AppDestinations.PIECE_EDIT.name}?imageUri=$encodedUri"
                            )
                        }
                    )
                }
                composable(AppDestinations.CLOSET.name) {
                    Closet(
                        modifier = Modifier.fillMaxSize(),
                        navController = navController
                    )
                }
                composable(AppDestinations.SETTINGS.name) {
                    SettingsScreen(
                        modifier = Modifier.fillMaxSize(),
                        navController = navController
                    )
                }
                composable(
                    route =
                    "${AppDestinations.PIECE_EDIT.name}?pieceId={pieceId}&imageUri={imageUri}",
                    arguments = listOf(
                        navArgument("pieceId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                        navArgument("imageUri") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val pieceId = backStackEntry.arguments?.getLong("pieceId")
                    val imageUri = backStackEntry.arguments?.getString("imageUri")
                    PieceEditScreen(
                        modifier = Modifier.fillMaxSize(),
                        pieceId = pieceId,
                        imageUri = imageUri,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}