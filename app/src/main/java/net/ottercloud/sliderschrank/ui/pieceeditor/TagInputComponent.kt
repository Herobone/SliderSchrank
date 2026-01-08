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
package net.ottercloud.sliderschrank.ui.pieceeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import net.ottercloud.sliderschrank.R

@Composable
fun TagInputComponent(state: PieceEditScreenState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        TagInputLabel()
        TagChipsList(state.completedTags, state.onTagsChange)
        TagInputField(state)
        TagSuggestions(state)
    }
}

@Composable
private fun TagInputLabel() {
    Text(
        text = stringResource(R.string.tags),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun TagChipsList(tags: List<String>, onRemove: (List<String>) -> Unit) {
    if (tags.isEmpty()) return

    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        tags.forEach { tag ->
            TagChip(tag) { onRemove(tags - tag) }
        }
    }
}

@Composable
private fun TagChip(tag: String, onRemove: () -> Unit) {
    InputChip(
        selected = false,
        onClick = onRemove,
        label = { Text(tag) },
        trailingIcon = {
            Icon(
                Icons.Default.Close,
                contentDescription = null
            )
        }
    )
}

@Composable
private fun TagInputField(state: PieceEditScreenState) {
    OutlinedTextField(
        value = state.currentTagInput,
        onValueChange = { newValue ->
            handleTagInput(newValue, state)
        },
        placeholder = { Text(stringResource(R.string.enter_tag_here)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
    )
}

@Composable
private fun TagSuggestions(state: PieceEditScreenState) {
    if (state.currentTagInput.isEmpty()) return

    val suggestions = filterTagSuggestions(state)
    if (suggestions.isEmpty()) return

    Spacer(modifier = Modifier.height(8.dp))

    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions.forEach { suggestion ->
            SuggestionChip(
                onClick = {
                    state.onTagsChange(state.completedTags + suggestion.name)
                    state.onTagInputChange("")
                },
                label = { Text(suggestion.name) }
            )
        }
    }
}

private fun handleTagInput(newValue: String, state: PieceEditScreenState) {
    val isTagTerminator = newValue.endsWith(",") || newValue.endsWith("\n")

    if (isTagTerminator) {
        val newTag = newValue.dropLast(1).trim()
        val isValid = newTag.isNotEmpty() && !state.completedTags.contains(newTag)

        if (isValid) {
            state.onTagsChange(state.completedTags + newTag)
        }
        state.onTagInputChange("")
    } else {
        state.onTagInputChange(newValue)
    }
}

private fun filterTagSuggestions(state: PieceEditScreenState) = state.allTags.filter {
    it.name.contains(state.currentTagInput, ignoreCase = true) &&
        !state.completedTags.contains(it.name)
}.take(10)