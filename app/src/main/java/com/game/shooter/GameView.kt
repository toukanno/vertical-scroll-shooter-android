package com.game.shooter

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var thread: Thread? = null
    private var running = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Screen dimensions
    private var screenW = 0f
    private var screenH = 0f

    // Game state
    private var gameState = GameState.TITLE
    private var player = Player(0f, 0f)
    private val bullets = mutableListOf<Bullet>()
    private val enemies = mutableListOf<Enemy>()
    private val enemyBullets = mutableListOf<EnemyBullet>()
    private val powerUps = mutableListOf<PowerUp>()
    private val stars = mutableListOf<Star>()
    private var explosions = mutableListOf<Explosion>()

    // Game timers
    private var bulletCooldown = 0
    private var enemySpawnTimer = 0
    private var enemyShootTimer = 0
    private var stageTimer = 0
    private var stage = 1
    private var bossSpawned = false
    private var boss: Enemy? = null

    // Touch handling
    private var touchX = 0f
    private var touchY = 0f
    private var isTouching = false
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var playerStartX = 0f
    private var playerStartY = 0f

    // Colors
    private val bgColor = Color.rgb(5, 5, 20)
    private val playerColor = Color.rgb(100, 200, 255)
    private val playerAccentColor = Color.rgb(0, 255, 255)
    private val bulletColor = Color.rgb(255, 255, 100)
    private val enemyColors = mapOf(
        EnemyType.BASIC to Color.rgb(255, 100, 100),
        EnemyType.FAST to Color.rgb(255, 180, 0),
        EnemyType.TANK to Color.rgb(180, 50, 180),
        EnemyType.BOSS to Color.rgb(255, 50, 50)
    )

    data class Explosion(var x: Float, var y: Float, var radius: Float, var maxRadius: Float, var alpha: Int = 255)

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenW = width.toFloat()
        screenH = height.toFloat()
        initGame()
        running = true
        thread = Thread(this)
        thread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenW = width.toFloat()
        screenH = height.toFloat()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        thread?.join()
    }

    private fun initGame() {
        player = Player(screenW / 2, screenH * 0.8f)
        bullets.clear()
        enemies.clear()
        enemyBullets.clear()
        powerUps.clear()
        explosions.clear()
        stage = 1
        bossSpawned = false
        boss = null
        stageTimer = 0
        enemySpawnTimer = 0
        initStars()
    }

    private fun initStars() {
        stars.clear()
        repeat(80) {
            stars.add(Star(
                x = Random.nextFloat() * screenW,
                y = Random.nextFloat() * screenH,
                speed = Random.nextFloat() * 4f + 1f,
                size = Random.nextFloat() * 3f + 1f
            ))
        }
    }

    override fun run() {
        val targetFps = 60L
        val frameTime = 1000L / targetFps

        while (running) {
            val startTime = System.currentTimeMillis()
            update()
            draw()
            val elapsed = System.currentTimeMillis() - startTime
            val sleepTime = frameTime - elapsed
            if (sleepTime > 0) Thread.sleep(sleepTime)
        }
    }

    private fun update() {
        when (gameState) {
            GameState.PLAYING -> updateGame()
            else -> {}
        }
        updateStars()
    }

    private fun updateStars() {
        for (star in stars) {
            star.y += star.speed
            if (star.y > screenH) {
                star.y = 0f
                star.x = Random.nextFloat() * screenW
            }
        }
    }

    private fun updateGame() {
        stageTimer++

        // Move player toward touch point
        if (isTouching) {
            val dx = touchX - player.x
            val dy = touchY - player.y
            val speed = 15f
            if (abs(dx) > 5f) player.x += dx.coerceIn(-speed, speed)
            if (abs(dy) > 5f) player.y += dy.coerceIn(-speed, speed)
            player.x = player.x.coerceIn(player.width / 2, screenW - player.width / 2)
            player.y = player.y.coerceIn(player.height / 2, screenH - player.height / 2)
        }

        // Auto-fire bullets
        if (bulletCooldown <= 0) {
            fireBullet()
            bulletCooldown = when (player.powerLevel) {
                1 -> 12
                2 -> 8
                else -> 5
            }
        } else {
            bulletCooldown--
        }

        // Update bullets
        bullets.removeAll { bullet ->
            bullet.y += bullet.speedY
            !bullet.active || bullet.y < -bullet.height
        }

        // Spawn enemies
        val spawnInterval = maxOf(30, 90 - stage * 10)
        if (!bossSpawned && enemySpawnTimer <= 0) {
            spawnEnemy()
            enemySpawnTimer = spawnInterval
        } else {
            enemySpawnTimer--
        }

        // Stage boss after 30 seconds
        if (!bossSpawned && stageTimer > 60 * 30) {
            bossSpawned = true
            spawnBoss()
        }

        // Update enemies
        for (enemy in enemies) {
            enemy.y += enemy.speed
            // Zigzag movement for fast enemies
            if (enemy.type == EnemyType.FAST) {
                enemy.moveTimer++
                enemy.x += Math.sin(enemy.moveTimer * 0.1) * 3
                enemy.x = enemy.x.coerceIn(enemy.width / 2, screenW - enemy.width / 2).toFloat()
            }
            // Boss side movement
            if (enemy.type == EnemyType.BOSS) {
                enemy.moveTimer++
                enemy.x = screenW / 2 + Math.sin(enemy.moveTimer * 0.02) * (screenW * 0.35)
                enemy.x = enemy.x.toFloat()
                boss = enemy
            }
        }
        enemies.removeAll { it.y > screenH + it.height || !it.active }

        // Enemy shooting
        enemyShootTimer++
        val shootInterval = maxOf(40, 80 - stage * 5)
        if (enemyShootTimer >= shootInterval) {
            enemyShootTimer = 0
            for (enemy in enemies.shuffled().take(2)) {
                enemyBullets.add(EnemyBullet(enemy.x, enemy.y + enemy.height / 2))
            }
            boss?.let { b ->
                // Boss fires spread shot
                for (angle in listOf(-0.3f, 0f, 0.3f)) {
                    enemyBullets.add(EnemyBullet(
                        b.x, b.y + b.height / 2,
                        speedX = Math.sin(angle.toDouble()).toFloat() * 5f,
                        speedY = Math.cos(angle.toDouble()).toFloat() * 6f
                    ))
                }
            }
        }

        // Update enemy bullets
        for (eb in enemyBullets) {
            eb.x += eb.speedX
            eb.y += eb.speedY
        }
        enemyBullets.removeAll { !it.active || it.y > screenH + it.height }

        // Update power-ups
        for (pu in powerUps) {
            pu.y += pu.speedY
        }
        powerUps.removeAll { !it.active || it.y > screenH + it.height }

        // Update explosions
        for (exp in explosions) {
            exp.radius += exp.maxRadius * 0.08f
            exp.alpha = (255 * (1f - exp.radius / exp.maxRadius)).toInt()
        }
        explosions.removeAll { it.alpha <= 0 }

        // Update invincibility
        if (player.invincible) {
            player.invincibleTimer--
            if (player.invincibleTimer <= 0) player.invincible = false
        }

        // Collision: player bullets vs enemies
        for (bullet in bullets) {
            for (enemy in enemies) {
                if (bullet.active && enemy.active && RectF.intersects(bullet.bounds, enemy.bounds)) {
                    bullet.active = false
                    enemy.hp--
                    if (enemy.hp <= 0) {
                        enemy.active = false
                        player.score += enemy.points
                        explosions.add(Explosion(enemy.x, enemy.y, 0f, enemy.width * 1.5f))
                        // Random power-up drop
                        if (Random.nextFloat() < 0.2f) {
                            powerUps.add(PowerUp(enemy.x, enemy.y, PowerUpType.values().random()))
                        }
                    }
                }
            }
        }

        // Collision: enemy bullets vs player
        if (!player.invincible) {
            for (eb in enemyBullets) {
                if (eb.active && RectF.intersects(eb.bounds, player.bounds)) {
                    eb.active = false
                    hitPlayer()
                }
            }
            // Enemy collision with player
            for (enemy in enemies) {
                if (enemy.active && RectF.intersects(enemy.bounds, player.bounds)) {
                    enemy.active = false
                    explosions.add(Explosion(enemy.x, enemy.y, 0f, enemy.width * 1.5f))
                    hitPlayer()
                }
            }
        }

        // Collect power-ups
        for (pu in powerUps) {
            if (pu.active && RectF.intersects(pu.bounds, player.bounds)) {
                pu.active = false
                when (pu.type) {
                    PowerUpType.LIFE -> player.lives = min(5, player.lives + 1)
                    PowerUpType.POWER -> player.powerLevel = min(3, player.powerLevel + 1)
                    PowerUpType.SHIELD -> {
                        player.invincible = true
                        player.invincibleTimer = 180
                    }
                }
            }
        }

        // Stage clear: boss defeated
        val bossDefeated = bossSpawned && (boss == null || !boss!!.active)
        if (bossDefeated && enemies.isEmpty()) {
            stage++
            bossSpawned = false
            boss = null
            stageTimer = 0
            gameState = GameState.STAGE_CLEAR
        }
    }

    private fun fireBullet() {
        when (player.powerLevel) {
            1 -> bullets.add(Bullet(player.x, player.y - player.height / 2))
            2 -> {
                bullets.add(Bullet(player.x - 15f, player.y - player.height / 2))
                bullets.add(Bullet(player.x + 15f, player.y - player.height / 2))
            }
            else -> {
                bullets.add(Bullet(player.x, player.y - player.height / 2))
                bullets.add(Bullet(player.x - 25f, player.y - player.height / 2 + 10f))
                bullets.add(Bullet(player.x + 25f, player.y - player.height / 2 + 10f))
            }
        }
    }

    private fun spawnEnemy() {
        val type = when {
            stage >= 3 && Random.nextFloat() < 0.2f -> EnemyType.TANK
            stage >= 2 && Random.nextFloat() < 0.3f -> EnemyType.FAST
            else -> EnemyType.BASIC
        }
        val hp = when (type) {
            EnemyType.BASIC -> 1
            EnemyType.FAST -> 1
            EnemyType.TANK -> 3 + stage
            EnemyType.BOSS -> 20 + stage * 5
        }
        enemies.add(Enemy(
            x = Random.nextFloat() * (screenW - 100f) + 50f,
            y = -100f,
            type = type,
            hp = hp
        ))
    }

    private fun spawnBoss() {
        val b = Enemy(
            x = screenW / 2,
            y = -200f,
            type = EnemyType.BOSS,
            hp = 20 + stage * 5
        )
        enemies.add(b)
        boss = b
    }

    private fun hitPlayer() {
        player.lives--
        explosions.add(Explosion(player.x, player.y, 0f, 100f))
        if (player.lives <= 0) {
            gameState = GameState.GAME_OVER
        } else {
            player.invincible = true
            player.invincibleTimer = 120
        }
    }

    private fun draw() {
        val canvas = holder.lockCanvas() ?: return
        try {
            // Background
            canvas.drawColor(bgColor)

            // Stars
            paint.color = Color.WHITE
            for (star in stars) {
                paint.alpha = (100 + star.speed * 30).toInt().coerceIn(0, 255)
                canvas.drawCircle(star.x, star.y, star.size, paint)
            }
            paint.alpha = 255

            when (gameState) {
                GameState.TITLE -> drawTitle(canvas)
                GameState.PLAYING -> drawGame(canvas)
                GameState.GAME_OVER -> { drawGame(canvas); drawGameOver(canvas) }
                GameState.STAGE_CLEAR -> { drawGame(canvas); drawStageClear(canvas) }
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawGame(canvas: Canvas) {
        // Power-ups
        for (pu in powerUps) {
            paint.color = when (pu.type) {
                PowerUpType.LIFE -> Color.RED
                PowerUpType.POWER -> Color.YELLOW
                PowerUpType.SHIELD -> Color.CYAN
            }
            canvas.drawCircle(pu.x, pu.y, pu.width / 2, paint)
            paint.color = Color.WHITE
            paint.textSize = 22f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(when (pu.type) {
                PowerUpType.LIFE -> "❤"
                PowerUpType.POWER -> "★"
                PowerUpType.SHIELD -> "◆"
            }, pu.x, pu.y + 8f, paint)
        }

        // Enemy bullets
        paint.color = Color.rgb(255, 80, 80)
        for (eb in enemyBullets) {
            if (!eb.active) continue
            canvas.drawOval(eb.x - eb.width/2, eb.y - eb.height/2, eb.x + eb.width/2, eb.y + eb.height/2, paint)
        }

        // Enemies
        for (enemy in enemies) {
            if (!enemy.active) continue
            paint.color = enemyColors[enemy.type] ?: Color.RED
            // Draw enemy ship shape
            val path = Path()
            path.moveTo(enemy.x, enemy.y + enemy.height / 2)
            path.lineTo(enemy.x - enemy.width / 2, enemy.y - enemy.height / 2)
            path.lineTo(enemy.x, enemy.y - enemy.height / 3)
            path.lineTo(enemy.x + enemy.width / 2, enemy.y - enemy.height / 2)
            path.close()
            canvas.drawPath(path, paint)

            // HP bar for tanks and boss
            if (enemy.type == EnemyType.TANK || enemy.type == EnemyType.BOSS) {
                val maxHp = if (enemy.type == EnemyType.BOSS) (20 + stage * 5) else (3 + stage)
                val barW = enemy.width * 1.2f
                val barH = 8f
                val barX = enemy.x - barW / 2
                val barY = enemy.y - enemy.height / 2 - 15f
                paint.color = Color.DKGRAY
                canvas.drawRect(barX, barY, barX + barW, barY + barH, paint)
                paint.color = Color.GREEN
                canvas.drawRect(barX, barY, barX + barW * enemy.hp / maxHp, barY + barH, paint)
            }
        }

        // Player bullets
        paint.color = bulletColor
        for (bullet in bullets) {
            if (!bullet.active) continue
            canvas.drawRect(
                bullet.x - bullet.width/2, bullet.y - bullet.height/2,
                bullet.x + bullet.width/2, bullet.y + bullet.height/2, paint
            )
        }

        // Player ship
        if (!player.invincible || stageTimer % 6 < 3) {
            // Engine glow
            paint.color = Color.rgb(0, 150, 255)
            canvas.drawOval(
                player.x - 15f, player.y + player.height / 2 - 10f,
                player.x + 15f, player.y + player.height / 2 + 20f, paint
            )
            // Ship body
            val pp = Path()
            pp.moveTo(player.x, player.y - player.height / 2)
            pp.lineTo(player.x - player.width / 2, player.y + player.height / 2)
            pp.lineTo(player.x - player.width / 4, player.y + player.height / 4)
            pp.lineTo(player.x + player.width / 4, player.y + player.height / 4)
            pp.lineTo(player.x + player.width / 2, player.y + player.height / 2)
            pp.close()
            paint.color = playerColor
            canvas.drawPath(pp, paint)
            // Cockpit
            paint.color = playerAccentColor
            canvas.drawCircle(player.x, player.y - player.height / 6, 12f, paint)
        }

        // Explosions
        for (exp in explosions) {
            paint.color = Color.argb(exp.alpha, 255, 200, 0)
            canvas.drawCircle(exp.x, exp.y, exp.radius, paint)
            paint.color = Color.argb(exp.alpha / 2, 255, 100, 0)
            canvas.drawCircle(exp.x, exp.y, exp.radius * 0.6f, paint)
        }

        // HUD
        drawHUD(canvas)
    }

    private fun drawHUD(canvas: Canvas) {
        paint.textSize = 40f
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.WHITE
        canvas.drawText("SCORE: ${player.score}", 20f, 55f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("STAGE: $stage", screenW - 20f, 55f, paint)

        // Lives
        paint.color = Color.RED
        paint.textSize = 35f
        paint.textAlign = Paint.Align.LEFT
        for (i in 0 until player.lives) {
            canvas.drawText("♥", 20f + i * 40f, 100f, paint)
        }

        // Power level
        paint.color = Color.YELLOW
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("PWR: ${"★".repeat(player.powerLevel)}", screenW - 20f, 100f, paint)
    }

    private fun drawTitle(canvas: Canvas) {
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.rgb(100, 200, 255)
        paint.textSize = 70f
        canvas.drawText("VERTICAL", screenW / 2, screenH * 0.3f, paint)
        canvas.drawText("SHOOTER", screenW / 2, screenH * 0.3f + 80f, paint)

        paint.color = Color.WHITE
        paint.textSize = 35f
        canvas.drawText("タップしてスタート", screenW / 2, screenH * 0.6f, paint)

        paint.textSize = 28f
        paint.color = Color.LTGRAY
        canvas.drawText("タップ: 移動  自動射撃", screenW / 2, screenH * 0.7f, paint)
    }

    private fun drawGameOver(canvas: Canvas) {
        paint.color = Color.argb(180, 0, 0, 0)
        canvas.drawRect(0f, 0f, screenW, screenH, paint)

        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.RED
        paint.textSize = 70f
        canvas.drawText("GAME OVER", screenW / 2, screenH * 0.4f, paint)

        paint.color = Color.WHITE
        paint.textSize = 45f
        canvas.drawText("SCORE: ${player.score}", screenW / 2, screenH * 0.55f, paint)

        paint.textSize = 35f
        canvas.drawText("タップしてリトライ", screenW / 2, screenH * 0.7f, paint)
    }

    private fun drawStageClear(canvas: Canvas) {
        paint.color = Color.argb(180, 0, 0, 0)
        canvas.drawRect(0f, 0f, screenW, screenH, paint)

        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.YELLOW
        paint.textSize = 60f
        canvas.drawText("STAGE ${stage - 1} CLEAR!", screenW / 2, screenH * 0.4f, paint)

        paint.color = Color.WHITE
        paint.textSize = 35f
        canvas.drawText("タップして次のステージへ", screenW / 2, screenH * 0.6f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchX = event.x
                touchY = event.y
                isTouching = true
                when (gameState) {
                    GameState.TITLE -> {
                        gameState = GameState.PLAYING
                        initGame()
                    }
                    GameState.GAME_OVER -> {
                        gameState = GameState.PLAYING
                        initGame()
                    }
                    GameState.STAGE_CLEAR -> {
                        gameState = GameState.PLAYING
                        bullets.clear()
                        enemies.clear()
                        enemyBullets.clear()
                        powerUps.clear()
                        explosions.clear()
                        stageTimer = 0
                        enemySpawnTimer = 0
                        bossSpawned = false
                        boss = null
                    }
                    else -> {}
                }
            }
            MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y
            }
            MotionEvent.ACTION_UP -> {
                isTouching = false
            }
        }
        return true
    }

    fun pause() {
        running = false
        thread?.join()
    }

    fun resume() {
        running = true
        thread = Thread(this)
        thread?.start()
    }
}
