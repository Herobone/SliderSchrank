package net.ottercloud.sliderschrank

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.ottercloud.sliderschrank.ui.theme.SliderSchrankTheme

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // Add padding for the system status bar
    ) {
        // Top action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Dein Outfit", style = MaterialTheme.typography.titleLarge)
            Row {
                IconButton(onClick = { /* TODO: Random Shuffle Action */ }) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Zufälliges Outfit")
                }
                IconButton(onClick = { /* TODO: Save as Favourite Action */ }) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Outfit speichern")
                }
            }
        }

        // Garment sliders
        Column(
            modifier = Modifier
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
    // This will be a horizontal pager later
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GarmentItem()
    }
}

@Composable
fun GarmentItem() {
    var isLocked by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = 250.dp, height = 150.dp) // Adjusted height slightly
    ) {
        // Placeholder for the garment image
        Card(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Kleidungsstück")
            }
        }

        // Lock Button
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
