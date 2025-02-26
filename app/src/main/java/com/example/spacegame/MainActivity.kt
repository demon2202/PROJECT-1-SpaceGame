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

data class Player(
    var lives: Int = 3,
    var isInvulnerable: Boolean = false,
    var invulnerabilityTimer: Long = 0L
)

@Composable
fun SpaceShooterGame() {
    // Game state variables
    var playerX by remember { mutableStateOf(200f) }
    var player by remember { mutableStateOf(Player()) }
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
    var isPaused by remember { mutableStateOf(false) }

    // Power-up timers
    var shieldTimer by remember { mutableStateOf(0L) }
    var tripleShotTimer by remember { mutableStateOf(0L) }

    // Game constants
    val ENEMY_SIZE = 16f
    val PLAYER_SIZE = 40f
    val BULLET_SIZE = 12f
    val BOSS_SIZE = 50f
    val ENEMY_SPAWN_RATE = 0.015f
    val ENEMY_SPEED = 2f
    val BULLET_SPEED = 25f
    val INVULNERABILITY_DURATION = 2000L // 2 seconds
    val POWER_UP_DURATION = 10000L // 10 seconds

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

    // Reset game function
    fun resetGame() {
        player = Player()
        gameOver = false
        score = 0
        enemies = listOf()
        bullets = listOf()
        enemyProjectiles = listOf()
        rewards = listOf()
        explosions = listOf()
        playerX = 200f
        bossActive = false
        shieldActive = false
        tripleShotActive = false
        shieldTimer = 0L
        tripleShotTimer = 0L
    }

    // Game loop
    LaunchedEffect(Unit) {
        var lastUpdateTime = System.currentTimeMillis()

        while (true) {
            if (!isPaused && !gameOver) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = currentTime - lastUpdateTime
                lastUpdateTime = currentTime

                // Update power-up timers
                if (shieldActive && currentTime > shieldTimer) {
                    shieldActive = false
                }
                if (tripleShotActive && currentTime > tripleShotTimer) {
                    tripleShotActive = false
                }

                // Update player invulnerability
                if (player.isInvulnerable && currentTime > player.invulnerabilityTimer) {
                    player.isInvulnerable = false
                }

                // Background scrolling
                backgroundOffset = (backgroundOffset + 3) % 800

                // Update bullets
                bullets = bullets.map { bullet ->
                    bullet.copy(position = bullet.position.copy(y = bullet.position.y - BULLET_SPEED))
                }.filter { it.position.y > 0 }

                // Update enemies with smoother movement
                enemies = enemies.map {
                    val dx = (Random.nextFloat() - 0.5f) * 2 * deltaTime / 16f
                    it.copy(
                        x = (it.x + dx).coerceIn(ENEMY_SIZE, 400f - ENEMY_SIZE),
                        y = it.y + ENEMY_SPEED * deltaTime / 16f
                    )
                }.filter { it.y < 800 }

                // Update enemy projectiles
                enemyProjectiles = enemyProjectiles.map {
                    it.copy(y = it.y + 5 * deltaTime / 16f)
                }.filter { it.y < 800 }

                // Update rewards
                rewards = rewards.map {
                    it.copy(y = it.y + 3 * deltaTime / 16f)
                }.filter { it.y < 800 }

                // Update explosions
                explosions = explosions.filter { Random.nextFloat() > 0.1 }

                // Boss logic with improved movement
                if (bossActive) {
                    bossPosition = bossPosition.copy(
                        x = (bossPosition.x + Math.sin(currentTime / 1000.0) * 2).toFloat()
                            .coerceIn(BOSS_SIZE/2, 400f - BOSS_SIZE/2)
                    )
                }

                // Spawn boss every 15 kills
                if (score % 150 == 0 && score > 0 && !bossActive) {
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

                // Boss shooting with pattern
                if (bossActive && Random.nextFloat() < 0.03) {
                    val projectiles = List(3) { index ->
                        val spread = (index - 1) * 20f
                        Offset(
                            bossPosition.x + BOSS_SIZE/2 + spread,
                            bossPosition.y + BOSS_SIZE
                        )
                    }
                    enemyProjectiles = enemyProjectiles + projectiles
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
                    if (Random.nextBoolean()) {
                        shieldActive = true
                        shieldTimer = currentTime + POWER_UP_DURATION
                    } else {
                        tripleShotActive = true
                        tripleShotTimer = currentTime + POWER_UP_DURATION
                    }
                }

                // Player hit detection with lives system
                if (!player.isInvulnerable && !shieldActive &&
                    enemyProjectiles.any {
                        it.distanceTo(Offset(playerX, 750f)) < PLAYER_SIZE/3
                    }) {
                    player.lives--
                    if (player.lives <= 0) {
                        gameOver = true
                        highestScore = maxOf(highestScore, score)
                    } else {
                        player.isInvulnerable = true
                        player.invulnerabilityTimer = currentTime + INVULNERABILITY_DURATION
                    }
                }
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
            // Score and lives display
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Score: $score",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "Lives: ${player.lives}",
                    color = Color.Red,
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

            // Power-up indicators
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                if (shieldActive) {
                    Text(
                        text = "Shield Active!",
                        color = Color.Cyan,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                if (tripleShotActive) {
                    Text(
                        text = "Triple Shot!",
                        color = Color.Yellow,
                        modifier = Modifier.padding(end = 8.dp)
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
                            if (!isPaused && !gameOver) {
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
                    }
            ) {
                // Draw player with blinking effect when invulnerable
                val playerAlpha = when {
                    player.isInvulnerable -> if ((System.currentTimeMillis() / 200) % 2 == 0L) 0.5f else 1f
                    shieldActive -> 0.7f
                    else -> 1f
                }
                drawImage(
                    playerImage,
                    Offset(playerX - PLAYER_SIZE/2, 720f - PLAYER_SIZE/2),
                    alpha = playerAlpha
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

                // Draw boss with health bar
                if (bossActive) {
                    drawImage(
                        bossImage,
                        Offset(bossPosition.x - BOSS_SIZE/2, bossPosition.y - BOSS_SIZE/2)
                    )

                    // Draw boss health bar
                    val healthBarWidth = 60f
                    val healthBarHeight = 8f
                    val healthPercentage = bossHealth / 8f

                    // Health bar background
                    drawRect(
                        color = Color.Red,
                        topLeft = Offset(bossPosition.x - healthBarWidth/2, bossPosition.y - BOSS_SIZE),
                        size = androidx.compose.ui.geometry.Size(width = healthBarWidth, height = healthBarHeight)
                    )

                    // Current health
                    drawRect(
                        color = Color.Green,
                        topLeft = Offset(bossPosition.x - healthBarWidth/2, bossPosition.y - BOSS_SIZE),
                        size = androidx.compose.ui.geometry.Size(width = healthBarWidth * healthPercentage, height = healthBarHeight)
                    )
                }

                // Draw enemy projectiles with glow effect
                enemyProjectiles.forEach {
                    // Glow
                    drawCircle(Color.Yellow.copy(alpha = 0.3f), radius = BULLET_SIZE, center = it)
                    // Core
                    drawCircle(Color.Yellow, radius = BULLET_SIZE/2, center = it)
                }

                // Draw rewards with pulsing effect
                rewards.forEach {
                    val pulseSize = (Math.sin(System.currentTimeMillis() / 200.0) * 2 + 10).toFloat()
                    drawCircle(Color.Cyan.copy(alpha = 0.3f), radius = pulseSize * 1.5f, center = it)
                    drawCircle(Color.Cyan, radius = pulseSize, center = it)
                }

                // Draw explosions with expanding effect
                explosions.forEach {
                    val explosionSize = (Math.random() * 10 + 15).toFloat()
                    drawCircle(Color(0xFFFF4444).copy(alpha = 0.7f), radius = explosionSize, center = it)
                    drawCircle(Color(0xFFFFAA00).copy(alpha = 0.5f), radius = explosionSize * 0.7f, center = it)
                }
            }
        }

        // Pause button
        Button(
            onClick = { isPaused = !isPaused },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Text(if (isPaused) "Resume" else "Pause")
        }

        // Game over overlay
        if (gameOver) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Game Over!", color = Color.Red, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Final Score: $score", color = Color.White, fontSize = 24.sp)
                Text("High Score: $highestScore", color = Color.Yellow, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { resetGame() }) {
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

        // Pause overlay
        if (isPaused) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Paused", color = Color.White, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { isPaused = false }) {
                    Text("Resume")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { resetGame(); isPaused = false }) {
                    Text("Restart")
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