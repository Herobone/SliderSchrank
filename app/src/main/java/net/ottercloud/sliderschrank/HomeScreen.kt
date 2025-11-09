package net.ottercloud.sliderschrank

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.ottercloud.sliderschrank.ui.theme.SliderSchrankTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dein Outfit") },
                actions = {
                    IconButton(onClick = { /* TODO: random Shuffle */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Zufälliges Outfit")
                    }
                    IconButton(onClick = { /* TODO: Save as Favourit */ }) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Outfit speichern")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GarmentSlider()
            GarmentSlider()
            GarmentSlider()
            GarmentSlider()
        }
    }
}

@Composable
fun GarmentSlider() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GarmentItem()
    }
}

@Composable
fun GarmentItem() {
    var isLocked by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = 250.dp, height = 150.dp)
    ) {
        // Placeholder for the garment image
        Card(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Kleidungsstück")
            }
        }

        IconButton(
            onClick = { isLocked = !isLocked },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Teil sperren",
                tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SliderSchrankTheme {
        HomeScreen()
    }
}
