package com.aura.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aura.ui.theme.AuraBody
import com.aura.ui.theme.AuraButtonLabel
import com.aura.ui.theme.AuraFieldRadius
import com.aura.ui.theme.V

@Composable
internal fun AuraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    VButton(
        text, onClick, modifier.heightIn(min = 58.dp),
        enabled = enabled,
        contentColor = V.ButtonText,
        labelStyle = AuraButtonLabel,
    )
}

@Composable
internal fun AuraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, style = AuraBody, color = V.Gray) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(AuraFieldRadius),
        textStyle = AuraBody,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = V.SurfaceRaised,
            unfocusedContainerColor = V.SurfaceRaised,
            focusedTextColor = V.White,
            unfocusedTextColor = V.White,
            cursorColor = V.Teal,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}


@Composable
internal fun AuraSearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    AuraTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = placeholder,
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = V.Gray) },
        trailingIcon = if (query.isNotEmpty()) {
            { AuraClearSearchButton { onQueryChange("") } }
        } else null,
    )
}

@Composable
internal fun AuraClearSearchButton(onClear: () -> Unit) {
    IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, "Clear search", tint = V.Gray) }
}
