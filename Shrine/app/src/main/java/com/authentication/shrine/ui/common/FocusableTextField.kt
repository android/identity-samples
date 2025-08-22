package com.authentication.shrine.ui.common

import android.content.res.Configuration
import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.Surface
import com.authentication.shrine.R
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

@Immutable
data class Padding(
    val start: Dp,
    val top: Dp,
    val end: Dp,
    val bottom: Dp,
)

val ParentPadding = PaddingValues(vertical = 16.dp, horizontal = 58.dp)

@Composable
fun rememberChildPadding(direction: LayoutDirection = LocalLayoutDirection.current): Padding {
    return remember {
        Padding(
            start = ParentPadding.calculateStartPadding(direction) + 8.dp,
            top = ParentPadding.calculateTopPadding(),
            end = ParentPadding.calculateEndPadding(direction) + 8.dp,
            bottom = ParentPadding.calculateBottomPadding()
        )
    }
}

val TvCardShape = ShapeDefaults.ExtraSmall

@Composable
fun FocusableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    placeholder: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val configuration = LocalConfiguration.current
    val isTelevision =
        (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

    Text(
        text = label,
        modifier = Modifier.padding(top = dimensionResource(R.dimen.dimen_standard)),
    )

    if (isTelevision) {
        val childPadding = rememberChildPadding()
        val tfFocusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val tfInteractionSource = remember { MutableInteractionSource() }
        val isTfFocused by tfInteractionSource.collectIsFocusedAsState()
        Surface(
            shape = ClickableSurfaceDefaults.shape(shape = TvCardShape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = TvMaterialTheme.colorScheme.inverseOnSurface,
                focusedContainerColor = TvMaterialTheme.colorScheme.inverseOnSurface,
                pressedContainerColor = TvMaterialTheme.colorScheme.inverseOnSurface,
                focusedContentColor = TvMaterialTheme.colorScheme.onSurface,
                pressedContentColor = TvMaterialTheme.colorScheme.onSurface
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(
                        width = if (isTfFocused) 2.dp else 1.dp,
                        color = animateColorAsState(
                            targetValue = if (isTfFocused) TvMaterialTheme.colorScheme.primary
                            else TvMaterialTheme.colorScheme.border,
                            label = ""
                        ).value
                    ),
                    shape = TvCardShape
                )
            ),
            tonalElevation = 2.dp,
            modifier = modifier
                .padding(horizontal = childPadding.start)
                .padding(top = 8.dp),
            onClick = { tfFocusRequester.requestFocus() }
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(tfFocusRequester)
                    .onKeyEvent {
                        if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                            when (it.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    focusManager.moveFocus(FocusDirection.Down)
                                }

                                KeyEvent.KEYCODE_DPAD_UP -> {
                                    focusManager.moveFocus(FocusDirection.Up)
                                }

                                KeyEvent.KEYCODE_BACK -> {
                                    focusManager.moveFocus(FocusDirection.Exit)
                                }
                            }
                        }
                        true
                    },
                interactionSource = tfInteractionSource,
                textStyle = TvMaterialTheme.typography.titleSmall.copy(
                    color = TvMaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = Brush.verticalGradient(
                    colors = listOf(
                        LocalContentColor.current,
                        LocalContentColor.current,
                    )
                ),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = TvMaterialTheme.colorScheme.surfaceVariant,
                                shape = TvMaterialTheme.shapes.small
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        leadingIcon?.invoke()
                        Spacer(modifier = Modifier.widthIn(min = 8.dp))
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = TvMaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )
        }
    } else {
        TextField(
            value = value,
            leadingIcon = leadingIcon,
            singleLine = true,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = modifier
        )
    }
}
