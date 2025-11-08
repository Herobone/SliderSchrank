package net.ottercloud.sliderschrank

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraEnhance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import net.ottercloud.sliderschrank.ui.theme.SliderSchrankTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SliderSchrankTheme {
                SliderSchrankApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun SliderSchrankApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        val modifier = if (destination == AppDestinations.KAMERA) {
                            Modifier.size(40.dp)
                        } else {
                            Modifier
                        }
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            modifier = modifier
                        )
                    },
                    label = if (destination != AppDestinations.KAMERA) {
                        { Text(destination.label) }
                    } else {
                        null
                    },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (currentDestination == AppDestinations.KLEIDERSCHRANK) {
                    TopAppBar(
                        title = { Text("Kleiderschrank") },
                        actions = {
                            IconButton(onClick = { /* TODO: settings button */ }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Einstellungen"
                                )
                            }
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination) {
                    AppDestinations.HOME -> Greeting(name = "Home")
                    AppDestinations.KAMERA -> Greeting(name = "Kamera")
                    AppDestinations.KLEIDERSCHRANK -> Kleiderschrank()
                }
            }
        }
    }
}

@Composable
fun Kleiderschrank() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Willkommen im Kleiderschrank!")
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    KAMERA("Kamera", Icons.Filled.CameraEnhance),
    KLEIDERSCHRANK("Kleiderschrank", Icons.Filled.Stars),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Hallo $name!",
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SliderSchrankTheme {
        Greeting("Android")
    }
}
