package net.ottercloud.sliderschrank

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.ottercloud.sliderschrank.ui.theme.SliderSchrankTheme

@Composable
fun CameraScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Hier kommt die Kamera hin!")
    }
}

@Preview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    SliderSchrankTheme {
        CameraScreen()
    }
}
