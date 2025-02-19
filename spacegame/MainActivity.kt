package com.example.spacegame

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpaceShooterGame()
        }
    }
}

data class Bullet(
    val position: Offset,
    val size: Float,
    val color: Color,
    val glowEffect: Boolean = true
) {
    fun distanceTo(other: Offset): Float = position.distanceTo(other)
}

@Composable
fun SpaceShooterGame() {
    // Game state variables
    var playerX by remember { mutableStateOf(200f) }
    var bullets by remember { mutableStateOf(listOf<Bullet>()) }
    var enemies by remember { mutableStateOf(listOf<Offset>()) }
    var enemyProjectiles by remember { mutableStateOf(listOf<Offset>()) }
    var rewards by remember { mutableStateOf(listOf<Offset>()) }
    var explosions by remember { mutableStateOf(listOf<Offset>()) }
    var score by remember { mutableStateOf(0) }
    var highestScore by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var bossActive by remember { mutableStateOf(false) }
    var bossHealth by remember { mutableStateOf(5) }
    var bossPosition by remember { mutableStateOf(Offset(200f, 50f)) }
    var shieldActive by remember { mutableStateOf(false) }
    var tripleShotActive by remember { mutableStateOf(false) }
    var backgroundOffset by remember { mutableStateOf(0f) }

    // Game constants
    val ENEMY_SIZE = 16f
    val PLAYER_SIZE = 40f
    val BULLET_SIZE = 12f
    val BOSS_SIZE = 50f
    val ENEMY_SPAWN_RATE = 0.015f
    val ENEMY_SPEED = 2f
    val BULLET_SPEED = 25f

    // Load images
    val playerImage = ImageBitmap.imageResource(id = R.drawable.spaceship)
    val enemyImage = ImageBitmap.imageResource(id = R.drawable.enemy)
    val bossImage = ImageBitmap.imageResource(id = R.drawable.boss)

    // Color palette for bullets
    val bulletColors = listOf(
        Color(0xFFFF0000), // Red
        Color(0xFFFF3366), // Pink
        Color(0xFFFF6600), // Orange
        Color(0xFFFFCC00)  // Yellow
    )

    // Game loop
    LaunchedEffect(Unit) {
        while (!gameOver) {
            // Background scrolling
            backgroundOffset = (backgroundOffset + 3) % 800

            // Update bullets
            bullets = bullets.map { bullet ->
                bullet.copy(position = bullet.position.copy(y = bullet.position.y - BULLET_SPEED))
            }.filter { it.position.y > 0 }

            // Update enemies
            enemies = enemies.map {
                it.copy(
                    x = (it.x + Random.nextInt(-2, 2)).coerceIn(ENEMY_SIZE, 400f - ENEMY_SIZE),
                    y = it.y + ENEMY_SPEED
                )
            }.filter { it.y < 800 }

            // Update enemy projectiles
            enemyProjectiles = enemyProjectiles.map {
                it.copy(y = it.y + 5)
            }.filter { it.y < 800 }

            // Update rewards
            rewards = rewards.map {
                it.copy(y = it.y + 3)
            }.filter { it.y < 800 }

            // Update explosions
            explosions = explosions.filter { Random.nextFloat() > 0.1 }

            // Boss logic
            if (score % 15 == 0 && score > 0 && !bossActive) {
                bossActive = true
                bossHealth = 8
                bossPosition = Offset(Random.nextFloat() * (400f - BOSS_SIZE), 50f)
            }

            // Spawn enemies
            if (!bossActive && Random.nextFloat() < ENEMY_SPAWN_RATE) {
                enemies = enemies + Offset(
                    Random.nextFloat() * (400f - ENEMY_SIZE * 2) + ENEMY_SIZE,
                    0f
                )
            }

            // Spawn rewards
            if (Random.nextFloat() < 0.02) {
                rewards = rewards + Offset(Random.nextFloat() * 400, 0f)
            }

            // Boss shooting
            if (bossActive && Random.nextFloat() < 0.03) {
                enemyProjectiles = enemyProjectiles + Offset(
                    bossPosition.x + BOSS_SIZE/2,
                    bossPosition.y + BOSS_SIZE
                )
            }

            // Collision detection - Bullets hitting enemies
            bullets = bullets.filter { bullet ->
                val hitEnemy = enemies.find { enemy ->
                    bullet.distanceTo(enemy) < ENEMY_SIZE + bullet.size/2
                }
                if (hitEnemy != null) {
                    explosions = explosions + hitEnemy
                    enemies = enemies - hitEnemy
                    score += 10
                    false
                } else true
            }

            // Boss collision detection
            if (bossActive) {
                bullets.find { bullet ->
                    bullet.distanceTo(
                        bossPosition.copy(x = bossPosition.x + BOSS_SIZE/2)
                    ) < BOSS_SIZE/1.5f
                }?.let {
                    bossHealth--
                    bullets = bullets - it
                    explosions = explosions + bossPosition
                    if (bossHealth <= 0) {
                        bossActive = false
                        score += 100
                    }
                }
            }

            // Power-up collection
            rewards.find {
                it.distanceTo(Offset(playerX, 750f)) < 25
            }?.let {
                rewards = rewards - it
                if (Random.nextBoolean()) shieldActive = true else tripleShotActive = true
            }

            // Player hit detection
            if (enemyProjectiles.any {
                    it.distanceTo(Offset(playerX, 750f)) < PLAYER_SIZE/3
                } && !shieldActive) {
                gameOver = true
                highestScore = maxOf(highestScore, score)
            }

            delay(16L)
        }
    }

    // Game UI
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.TopCenter
    ) {
        Column {
            // Score display
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Score: $score",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
                if (gameOver) {
                    Text(
                        text = "High Score: $highestScore",
                        color = Color.Yellow,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Game canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            playerX = (playerX + dragAmount.x).coerceIn(
                                PLAYER_SIZE/2,
                                400f - PLAYER_SIZE/2
                            )
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            val newBullets = if (tripleShotActive) {
                                listOf(
                                    Bullet(
                                        Offset(playerX - 15, 720f),
                                        BULLET_SIZE,
                                        bulletColors[Random.nextInt(bulletColors.size)]
                                    ),
                                    Bullet(
                                        Offset(playerX, 720f),
                                        BULLET_SIZE,
                                        bulletColors[Random.nextInt(bulletColors.size)]
                                    ),
                                    Bullet(
                                        Offset(playerX + 15, 720f),
                                        BULLET_SIZE,
                                        bulletColors[Random.nextInt(bulletColors.size)]
                                    )
                                )
                            } else {
                                listOf(
                                    Bullet(
                                        Offset(playerX, 720f),
                                        BULLET_SIZE,
                                        bulletColors[Random.nextInt(bulletColors.size)]
                                    )
                                )
                            }
                            bullets = bullets + newBullets
                        }
                    }
            ) {
                // Draw player
                drawImage(
                    playerImage,
                    Offset(playerX - PLAYER_SIZE/2, 720f - PLAYER_SIZE/2),
                    alpha = if (shieldActive) 0.7f else 1f
                )

                // Draw bullets with glow effects
                bullets.forEach { bullet ->
                    // Glow effect
                    drawCircle(
                        color = bullet.color.copy(alpha = 0.3f),
                        radius = bullet.size * 1.5f,
                        center = bullet.position
                    )
                    // Main bullet
                    drawCircle(
                        color = bullet.color,
                        radius = bullet.size,
                        center = bullet.position
                    )
                }

                // Draw enemies
                enemies.forEach { enemy ->
                    drawImage(
                        enemyImage,
                        Offset(enemy.x - ENEMY_SIZE/2, enemy.y - ENEMY_SIZE/2)
                    )
                }

                // Draw boss
                if (bossActive) {
                    drawImage(
                        bossImage,
                        Offset(bossPosition.x - BOSS_SIZE/2, bossPosition.y - BOSS_SIZE/2)
                    )
                }

                // Draw enemy projectiles
                enemyProjectiles.forEach {
                    drawCircle(Color.Yellow, radius = BULLET_SIZE/2, center = it)
                }

                // Draw rewards
                rewards.forEach {
                    drawCircle(Color.Cyan, radius = 10f, center = it)
                }

                // Draw explosions
                explosions.forEach {
                    drawCircle(Color(0xFFFF4444), radius = 20f, center = it)
                }
            }
        }

        // Game over overlay
        if (gameOver) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Game Over!", color = Color.Red, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    gameOver = false
                    score = 0
                    enemies = listOf()
                    bullets = listOf()
                    enemyProjectiles = listOf()
                    playerX = 200f
                }) {
                    Text("Play Again")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    // Handle exit - you can implement system exit or navigation here
                }) {
                    Text("Exit")
                }
            }
        }
    }
}

// Helper function
fun Offset.distanceTo(other: Offset) = hypot(x - other.x, y - other.y)
