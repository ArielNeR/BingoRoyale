package com.bingoroyale.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bingoroyale.ui.theme.*

// Tarjeta con efecto Glass
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassBackground
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = listOf(GlassBorder, Color.Transparent)
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

// Botón principal con gradiente
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    gradient: Brush = Brush.horizontalGradient(
        colors = listOf(RedPrimary, RedDark)
    ),
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled) gradient else Brush.horizontalGradient(
                        colors = listOf(Color.Gray, Color.DarkGray)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.invoke()
                if (icon != null) Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// Bola de Bingo animada
@Composable
fun BingoBall(
    number: Int,
    mode: Int = 75,
    size: BallSize = BallSize.LARGE,
    animated: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val color = getBallColor(number, mode)
    val letter = getBallLetter(number, mode)

    val scale by animateFloatAsState(
        targetValue = if (animated) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ball_scale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (animated) 0.5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val ballSize = when (size) {
        BallSize.SMALL -> 32.dp
        BallSize.MEDIUM -> 48.dp
        BallSize.LARGE -> 96.dp
    }

    val fontSize = when (size) {
        BallSize.SMALL -> 12.sp
        BallSize.MEDIUM -> 16.sp
        BallSize.LARGE -> 32.sp
    }

    Box(
        modifier = Modifier
            .size(ballSize)
            .then(if (animated) Modifier.shadow(
                elevation = (16 * glowAlpha).dp,
                shape = CircleShape,
                spotColor = color
            ) else Modifier)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0.7f))
                )
            )
            .border(
                width = 3.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (letter.isNotEmpty() && size == BallSize.LARGE) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Text(
                text = number.toString(),
                fontFamily = Orbitron,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

enum class BallSize {
    SMALL, MEDIUM, LARGE
}

// Celda del cartón de bingo
@Composable
fun BingoCell(
    value: String,
    isMarked: Boolean,
    isFreeSpace: Boolean = false,
    isHighlighted: Boolean = false,
    canMark: Boolean = false,
    onClick: () -> Unit = {}
) {
    val backgroundColor = when {
        isFreeSpace -> Brush.linearGradient(listOf(GoldPrimary, OrangePrimary))
        isMarked -> Brush.linearGradient(listOf(RedPrimary, OrangePrimary))
        else -> Brush.linearGradient(listOf(GlassBackground, GlassBackground))
    }

    val scale by animateFloatAsState(
        targetValue = if (isMarked) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "cell_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> GoldPrimary
            canMark -> GoldPrimary.copy(alpha = 0.5f)
            else -> GlassBorder
        },
        animationSpec = tween(300),
        label = "border_color"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isHighlighted) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !isFreeSpace && (canMark || isMarked), onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isFreeSpace) {
            Text(
                text = "⭐",
                fontSize = 24.sp
            )
        } else if (isMarked) {
            Text(
                text = "✓",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        } else {
            Text(
                text = value,
                fontFamily = Orbitron,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Header BINGO
@Composable
fun BingoHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(
            "B" to LetterB,
            "I" to LetterI,
            "N" to LetterN,
            "G" to LetterG,
            "O" to LetterO
        ).forEach { (letter, color) ->
            Text(
                text = letter,
                fontFamily = Orbitron,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Indicador de conexión
@Composable
fun ConnectionIndicator(
    isConnected: Boolean,
    clientCount: Int = 0,
    serverMode: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (isConnected) SuccessGreen else ErrorRed)
        )

        Text(
            text = if (serverMode) {
                if (isConnected) "$clientCount conectados" else "Servidor detenido"
            } else {
                if (isConnected) "Conectado" else "Desconectado"
            },
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
    }
}

// Botón de velocidad
@Composable
fun SpeedButton(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) GoldPrimary else GlassBackground
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Text(
            text = "${speed}x",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) BackgroundPrimary else TextPrimary
        )
    }
}

// Funciones auxiliares
fun getBallColor(number: Int, mode: Int): Color {
    return if (mode == 75) {
        when {
            number <= 15 -> BallRed
            number <= 30 -> BallOrange
            number <= 45 -> BallYellow
            number <= 60 -> BallGreen
            else -> BallBlue
        }
    } else {
        when {
            number <= 30 -> BallRed
            number <= 60 -> BallYellow
            else -> BallBlue
        }
    }
}

fun getBallLetter(number: Int, mode: Int): String {
    return if (mode == 75) {
        when {
            number <= 15 -> "B"
            number <= 30 -> "I"
            number <= 45 -> "N"
            number <= 60 -> "G"
            else -> "O"
        }
    } else ""
}