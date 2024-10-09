package ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.RadioButtonColors
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.material.ripple
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VerticalRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    height: Dp = 48.dp,
    connectTop: Boolean = true,
    connectBottom: Boolean = true,
) {
    val dotRadius = animateDpAsState(
        targetValue = if (selected) RadioButtonDotSize / 2 else 0.dp,
        animationSpec = tween(durationMillis = RadioAnimationDuration)
    )
    val radioColor = colors.radioColor(enabled, selected)
    val selectableModifier =
        if (onClick != null) {
            Modifier.selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = RadioButtonRippleRadius
                )
            )
        } else {
            Modifier
        }

//    var heightPx by remember {
//        mutableStateOf(0f)
//    }

    Canvas(
        modifier
            .then(
                if (onClick != null) {
                    Modifier.minimumInteractiveComponentSize()
                } else {
                    Modifier
                }
            )
            .then(selectableModifier)
//            .wrapContentSize(Alignment.Center)
            .padding(RadioButtonPadding)
            .requiredSize(RadioButtonSize)
//            .onGloballyPositioned { coordinates ->
//                // Set column height using the LayoutCoordinates
//                heightPx = coordinates.size.height.toFloat()
//            }
    ) {
        // Draw the radio button
        val strokeWidth = RadioStrokeWidth.toPx()
        val radioRadius = RadioRadius.toPx()
        val lineOverdraw = ((height.toPx() / 2) - radioRadius)
        if(connectTop) {
            drawLine(
                radioColor.value,
                start = Offset(center.x, -lineOverdraw),
                end = Offset(center.x, center.y - radioRadius),
                strokeWidth = strokeWidth,
            )
        }
        if(connectBottom) {
            drawLine(
                radioColor.value,
                start = Offset(center.x, center.y + radioRadius),
                end = Offset(center.x, size.height+lineOverdraw),
                strokeWidth = strokeWidth,
            )
        }
        drawCircle(
            radioColor.value,
            radioRadius - strokeWidth / 2,
            style = Stroke(strokeWidth)
        )
        if (dotRadius.value > 0.dp) {
            drawCircle(radioColor.value, dotRadius.value.toPx() - strokeWidth / 2, style = Fill)
        }
    }
}

@Composable
fun HorizontalRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    width: Dp = 48.dp,
    connectStart: Boolean = true,
    connectEnd: Boolean = true,
) {
    val dotRadius = animateDpAsState(
        targetValue = if (selected) RadioButtonDotSize / 2 else 0.dp,
        animationSpec = tween(durationMillis = RadioAnimationDuration)
    )
    val radioColor = colors.radioColor(enabled, selected)
    val selectableModifier =
        if (onClick != null) {
            Modifier.selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = RadioButtonRippleRadius
                )
            )
        } else {
            Modifier
        }

//    var widthPx by remember {
//        mutableStateOf(0f)
//    }

    Canvas(
        modifier
            .then(
                if (onClick != null) {
                    Modifier.minimumInteractiveComponentSize()
                } else {
                    Modifier
                }
            )
            .then(selectableModifier)
//            .wrapContentSize(Alignment.Center)
            .padding(RadioButtonPadding)
            .requiredSize(RadioButtonSize)
//            .onGloballyPositioned { coordinates ->
//                // Set column height using the LayoutCoordinates
//                widthPx = coordinates.size.height.toFloat()
//            }
    ) {
        // Draw the radio button
        val strokeWidth = RadioStrokeWidth.toPx()
        val radioRadius = RadioRadius.toPx()
        val lineOverdraw = ((width.toPx() / 2) - radioRadius)
        if(connectStart) {
            drawLine(
                radioColor.value,
                start = Offset( -lineOverdraw, center.y,),
                end = Offset(center.x - radioRadius, center.y),
                strokeWidth = strokeWidth,
            )
        }
        if(connectEnd) {
            drawLine(
                radioColor.value,
                start = Offset(center.x + radioRadius, center.y ),
                end = Offset(size.width + lineOverdraw, center.y),
                strokeWidth = strokeWidth,
            )
        }
        drawCircle(
            radioColor.value,
            radioRadius - strokeWidth / 2,
            style = Stroke(strokeWidth)
        )
        if (dotRadius.value > 0.dp) {
            drawCircle(radioColor.value, dotRadius.value.toPx() - strokeWidth / 2, style = Fill)
        }
    }
}

private const val RadioAnimationDuration = 100

private val RadioButtonRippleRadius = 24.dp
private val RadioButtonPadding = 2.dp
private val RadioButtonSize = 20.dp
private val RadioRadius = RadioButtonSize / 2
private val RadioButtonDotSize = 12.dp
private val RadioStrokeWidth = 2.dp