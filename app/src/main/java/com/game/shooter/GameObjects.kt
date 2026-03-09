package com.game.shooter

import android.graphics.RectF

// Player ship
data class Player(
    var x: Float,
    var y: Float,
    val width: Float = 80f,
    val height: Float = 100f,
    var lives: Int = 3,
    var score: Int = 0,
    var invincible: Boolean = false,
    var invincibleTimer: Int = 0,
    var powerLevel: Int = 1
) {
    val bounds get() = RectF(x - width / 2, y - height / 2, x + width / 2, y + height / 2)
}

// Player bullet
data class Bullet(
    var x: Float,
    var y: Float,
    val width: Float = 10f,
    val height: Float = 25f,
    val speedY: Float = -22f,
    var active: Boolean = true
) {
    val bounds get() = RectF(x - width / 2, y - height / 2, x + width / 2, y + height / 2)
}

// Enemy ship
data class Enemy(
    var x: Float,
    var y: Float,
    val type: EnemyType = EnemyType.BASIC,
    var hp: Int = 1,
    var active: Boolean = true,
    var moveTimer: Int = 0
) {
    val width get() = when (type) {
        EnemyType.BASIC -> 60f
        EnemyType.FAST -> 45f
        EnemyType.TANK -> 90f
        EnemyType.BOSS -> 160f
    }
    val height get() = when (type) {
        EnemyType.BASIC -> 70f
        EnemyType.FAST -> 55f
        EnemyType.TANK -> 85f
        EnemyType.BOSS -> 180f
    }
    val speed get() = when (type) {
        EnemyType.BASIC -> 4f
        EnemyType.FAST -> 7f
        EnemyType.TANK -> 2f
        EnemyType.BOSS -> 1.5f
    }
    val points get() = when (type) {
        EnemyType.BASIC -> 100
        EnemyType.FAST -> 150
        EnemyType.TANK -> 200
        EnemyType.BOSS -> 1000
    }
    val bounds get() = RectF(x - width / 2, y - height / 2, x + width / 2, y + height / 2)
}

enum class EnemyType { BASIC, FAST, TANK, BOSS }

// Enemy bullet
data class EnemyBullet(
    var x: Float,
    var y: Float,
    val speedX: Float = 0f,
    val speedY: Float = 8f,
    val width: Float = 10f,
    val height: Float = 20f,
    var active: Boolean = true
) {
    val bounds get() = RectF(x - width / 2, y - height / 2, x + width / 2, y + height / 2)
}

// Power-up item
data class PowerUp(
    var x: Float,
    var y: Float,
    val type: PowerUpType,
    var active: Boolean = true,
    val width: Float = 40f,
    val height: Float = 40f,
    val speedY: Float = 3f
) {
    val bounds get() = RectF(x - width / 2, y - height / 2, x + width / 2, y + height / 2)
}

enum class PowerUpType { LIFE, POWER, SHIELD }

// Scrolling star for background
data class Star(
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float
)

enum class GameState { TITLE, PLAYING, GAME_OVER, STAGE_CLEAR }
