package com.veyra.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.veyra.ui.theme.AuraBody
import com.veyra.ui.theme.AuraButtonLabel
import com.veyra.ui.theme.AuraFieldRadius
import com.veyra.ui.theme.V

@Composable
internal fun AuraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    VButton(
        text, onClick, modifier.height(58.dp),
        enabled = enabled,
        contentColor = V.Black,
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
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, style = AuraBody, color = V.Gray) },
        leadingIcon = leadingIcon,
        singleLine = true,
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
