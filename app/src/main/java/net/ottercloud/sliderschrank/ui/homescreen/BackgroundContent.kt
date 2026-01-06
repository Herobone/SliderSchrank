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
package net.ottercloud.sliderschrank.ui.homescreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import net.ottercloud.sliderschrank.AppBackground
import net.ottercloud.sliderschrank.CheckedBackground
import net.ottercloud.sliderschrank.R

@Suppress("ComposableModifierMissing")
@Composable
fun BackgroundContent(background: AppBackground, modifier: Modifier = Modifier) {
    when (background) {
        AppBackground.CORK -> {
            Image(
                painter = painterResource(id = R.drawable.kork),
                contentDescription = null,
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        AppBackground.GRAY -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Gray)
            )
        }
        AppBackground.WHITE -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }
        AppBackground.CHECKERED -> {
            CheckedBackground(modifier = modifier.fillMaxSize())
        }
    }
}