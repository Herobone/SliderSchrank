package net.ottercloud.sliderschrank

import androidx.compose.foundation.ExperimentalFoundationApi // <-- Import for Pager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager // <-- Import for Pager
import androidx.compose.foundation.pager.rememberPagerState // <-- Import for Pager
import androidx.compose.foundation.rememberScrollState // <-- Import for Vertical Scroll
import androidx.compose.foundation.verticalScroll // <-- Import for Vertical Scroll
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
    val groupedGarments = remember { dummyGarments.groupBy { it.type } }

    val categoryOrder = listOf(
        GarmentType.HEAD,
        GarmentType.TOP,
        GarmentType.BOTTOM,
        GarmentType.FEET
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Iterate over the defined category order
            categoryOrder.forEach { category ->
                // Get the list of garments for the current category
                val garmentsForCategory = groupedGarments[category].orEmpty()

                // Only display the slider if there are items in that category
                if (garmentsForCategory.isNotEmpty()) {
                    GarmentSlider(garments = garmentsForCategory)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class) // <-- Required for HorizontalPager
@Composable
fun GarmentSlider(garments: List<Garment>) { // <-- 'Garment' is from GarmentData.kt
    // State for the horizontal pager
    val pagerState = rememberPagerState(pageCount = { garments.size })

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // This is the horizontal pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
        ) { page ->
            // The content for each page is a GarmentItem
            // It's common to center the item in the pager's page
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                GarmentItem(garment = garments[page])
            }
        }
    }
}

@Composable
fun GarmentItem(garment: Garment) { // <-- 'Garment' is from GarmentData.kt
    var isLocked by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = 250.dp, height = 150.dp) // Adjusted height slightly
    ) {
        // Placeholder for the garment image
        Card(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(garment.name) // <-- Display the garment's name
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