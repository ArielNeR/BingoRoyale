package com.bingoroyale.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bingoroyale.ui.components.GlassCard
import com.bingoroyale.ui.components.GradientButton
import com.bingoroyale.ui.theme.*

@Composable
fun HomeScreen(
    onCallerClick: () -> Unit,
    onPlayerClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BackgroundPrimary,
                        BackgroundPurple,
                        BackgroundPrimary
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo animado
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GoldPrimary, RedPrimary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎱",
                    fontSize = 56.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Título
            Text(
                text = "BINGO ROYALE",
                fontFamily = Orbitron,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                style = MaterialTheme.typography.displayMedium.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(GoldPrimary, RedPrimary, GoldPrimary)
                    )
                )
            )

            Text(
                text = "Premium Experience",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Opción Cantador
            RoleCard(
                emoji = "🎤",
                title = "SOY CANTADOR",
                subtitle = "Genera y transmite los números",
                description = "Ideal para quien dirige el juego",
                gradient = Brush.linearGradient(
                    colors = listOf(RedPrimary, OrangePrimary)
                ),
                onClick = onCallerClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Opción Jugador
            RoleCard(
                emoji = "🎮",
                title = "SOY JUGADOR",
                subtitle = "Juega con tu cartón de bingo",
                description = "Marca los números y gana",
                gradient = Brush.linearGradient(
                    colors = listOf(GoldDark, GoldPrimary)
                ),
                onClick = onPlayerClick
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botón de configuración
            TextButton(
                onClick = onSettingsClick
            ) {
                Text(
                    text = "⚙️ Configuración",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info
            Text(
                text = "Juega en red local o de manera offline",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RoleCard(
    emoji: String,
    title: String,
    subtitle: String,
    description: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassBackground
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            GlassBackground
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 36.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontFamily = Orbitron,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Text(
                    text = "→",
                    fontSize = 24.sp,
                    color = TextMuted
                )
            }
        }
    }
}