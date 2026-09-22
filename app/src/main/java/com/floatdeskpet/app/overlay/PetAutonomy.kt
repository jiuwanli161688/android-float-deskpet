package com.floatdeskpet.app.overlay

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

internal class PetAutonomy {
    enum class Result { MOVE, IDLE, NAP }

    private enum class Kind { DIAGONAL, HOP, PAUSE_TURN, ARC, TWO_SEG, TARGET }

    private var kind = Kind.DIAGONAL
    private var until = 0L
    private var vx = 0f
    private var vy = 0f
    private var accX = 0f
    private var accY = 0f
    private var hopPhase = 0f
    private var hopAmp = 8f
    private var hopBaseY = 0
    private var pauseAt = 0L
    private var pausing = false
    private var turned = false
    private var arcCx = 0f
    private var arcCy = 0f
    private var arcR = 48f
    private var arcA = 0f
    private var arcDa = 0.01f
    private var wayX = 0
    private var wayY = 0
    private var destX = 0
    private var destY = 0
    private var seg = 0
    var active = false
        private set

    fun begin(x: Int, y: Int, b: WalkBounds, now: Long) {
        active = true
        accX = 0f
        accY = 0f
        pausing = false
        turned = false
        seg = 0
        pick(x, y, b, now)
    }

    fun cancel() {
        active = false
    }

    fun step(x: Int, y: Int, b: WalkBounds, now: Long): Triple<Int, Int, Result> {
        if (!active) return Triple(x, y, Result.IDLE)
        var nx = x
        var ny = y
        when (kind) {
            Kind.DIAGONAL -> {
                if (now >= until) return finish(nx, ny, nap = false)
                val moved = applyVel(nx, ny, b, bounce = true)
                nx = moved.first
                ny = moved.second
            }
            Kind.HOP -> {
                if (now >= until) return finish(nx, ny, nap = false)
                accX += vx
                val dx = accX.toInt()
                accX -= dx
                nx += dx
                if (nx <= b.minX) {
                    nx = b.minX
                    vx = abs(vx)
                } else if (nx >= b.maxX) {
                    nx = b.maxX
                    vx = -abs(vx)
                }
                hopPhase += 0.30f
                ny = (hopBaseY + (sin(hopPhase.toDouble()).toFloat() * hopAmp).toInt())
                    .coerceIn(b.minY, b.maxY)
            }
            Kind.PAUSE_TURN -> {
                if (now >= until) return finish(nx, ny, nap = false)
                if (pausing) {
                    if (now >= pauseAt) {
                        pausing = false
                        turned = true
                        vx = -vx
                        vy = (Random.nextFloat() - 0.5f) * 1.4f
                    }
                } else {
                    val moved = applyVel(nx, ny, b, bounce = true)
                    nx = moved.first
                    ny = moved.second
                    if (!turned && now >= pauseAt) {
                        pausing = true
                        pauseAt = now + (420L..900L).random()
                    }
                }
            }
            Kind.ARC -> {
                if (now >= until) return finish(nx, ny, nap = false)
                arcA += arcDa
                nx = (arcCx + cos(arcA) * arcR).toInt().coerceIn(b.minX, b.maxX)
                ny = (arcCy + sin(arcA) * arcR * 0.52f).toInt().coerceIn(b.minY, b.maxY)
            }
            Kind.TWO_SEG -> {
                if (now >= until) return finish(nx, ny, nap = false)
                val tx = if (seg == 0) wayX else destX
                val ty = if (seg == 0) wayY else destY
                val reached = seek(nx, ny, tx, ty, 3.1f)
                nx = reached.first
                ny = reached.second
                if (reached.third) {
                    if (seg == 0) {
                        seg = 1
                    } else {
                        return finish(nx, ny, nap = Random.nextFloat() < 0.35f)
                    }
                }
            }
            Kind.TARGET -> {
                if (now >= until) return finish(nx, ny, nap = Random.nextFloat() < 0.45f)
                val reached = seek(nx, ny, destX, destY, 3.0f)
                nx = reached.first
                ny = reached.second
                if (reached.third) return finish(nx, ny, nap = Random.nextFloat() < 0.62f)
            }
        }
        lastFaceRight = facingRight(nx, x)
        return Triple(nx, ny, Result.MOVE)
    }

    var lastFaceRight = true
        private set

    private fun facingRight(x: Int, prevX: Int): Boolean {
        val dx = x - prevX
        return when {
            dx > 0 -> true
            dx < 0 -> false
            vx > 0.05f -> true
            vx < -0.05f -> false
            kind == Kind.ARC -> -sin(arcA) * arcDa > 0f
            else -> lastFaceRight
        }
    }

    private fun pick(x: Int, y: Int, b: WalkBounds, now: Long) {
        val roll = Random.nextFloat()
        when {
            roll < 0.22f -> {
                kind = Kind.DIAGONAL
                val speed = 2.2f + Random.nextFloat() * 1.5f
                val ang = Random.nextFloat() * (Math.PI.toFloat() * 2f)
                vx = cos(ang) * speed
                vy = sin(ang) * speed * 0.62f
                if (abs(vx) < 0.8f) vx = if (vx >= 0f) 1.2f else -1.2f
                until = now + (7000L..13000L).random()
            }
            roll < 0.40f -> {
                kind = Kind.HOP
                vx = (if (Random.nextBoolean()) 1f else -1f) * (2.4f + Random.nextFloat() * 1.3f)
                vy = 0f
                hopPhase = 0f
                hopAmp = (7..14).random().toFloat()
                val amp = hopAmp.toInt()
                hopBaseY = if (b.maxY - b.minY < amp * 2) {
                    y.coerceIn(b.minY, b.maxY)
                } else {
                    y.coerceIn(b.minY + amp, b.maxY - amp)
                }
                until = now + (6000L..11000L).random()
            }
            roll < 0.56f -> {
                kind = Kind.PAUSE_TURN
                vx = (if (x > (b.minX + b.maxX) / 2) -1f else 1f) * (2.6f + Random.nextFloat() * 1.1f)
                vy = (Random.nextFloat() - 0.5f) * 0.8f
                pauseAt = now + (1800L..3600L).random()
                pausing = false
                turned = false
                until = now + (8000L..13000L).random()
            }
            roll < 0.72f -> {
                kind = Kind.ARC
                val spanX = ((b.maxX - b.minX) / 5).coerceAtLeast(36)
                val spanY = ((b.maxY - b.minY) / 6).coerceAtLeast(28)
                val cap = minOf(90, spanX, spanY).coerceAtLeast(37)
                arcR = (36..cap).random().toFloat()
                arcCx = x.toFloat()
                arcCy = y.toFloat()
                arcA = Random.nextFloat() * (Math.PI.toFloat() * 2f)
                arcDa = (if (Random.nextBoolean()) 1f else -1f) * (0.007f + Random.nextFloat() * 0.007f)
                until = now + (5000L..8000L).random()
            }
            roll < 0.86f -> {
                kind = Kind.TWO_SEG
                val mid = randAway(x, y, b, 70)
                wayX = mid.first
                wayY = mid.second
                val end = randAway(wayX, wayY, b, 70)
                destX = end.first
                destY = end.second
                seg = 0
                until = now + 16_000L
            }
            else -> {
                kind = Kind.TARGET
                val t = randAway(x, y, b, 90)
                destX = t.first
                destY = t.second
                until = now + 18_000L
            }
        }
        lastFaceRight = when (kind) {
            Kind.TARGET -> destX >= x
            Kind.TWO_SEG -> wayX >= x
            else -> vx >= 0f
        }
    }

    private fun applyVel(x: Int, y: Int, b: WalkBounds, bounce: Boolean): Pair<Int, Int> {
        accX += vx
        accY += vy
        val dx = accX.toInt()
        val dy = accY.toInt()
        accX -= dx
        accY -= dy
        var nx = x + dx
        var ny = y + dy
        if (bounce) {
            if (nx <= b.minX) {
                nx = b.minX
                vx = abs(vx)
            } else if (nx >= b.maxX) {
                nx = b.maxX
                vx = -abs(vx)
            }
            if (ny <= b.minY) {
                ny = b.minY
                vy = abs(vy)
            } else if (ny >= b.maxY) {
                ny = b.maxY
                vy = -abs(vy)
            }
        } else {
            nx = nx.coerceIn(b.minX, b.maxX)
            ny = ny.coerceIn(b.minY, b.maxY)
        }
        return nx to ny
    }

    private fun seek(x: Int, y: Int, tx: Int, ty: Int, speed: Float): Triple<Int, Int, Boolean> {
        val dx = (tx - x).toFloat()
        val dy = (ty - y).toFloat()
        val dist = hypot(dx, dy)
        if (dist <= speed + 1.5f) {
            vx = dx
            vy = dy
            return Triple(tx, ty, true)
        }
        vx = dx / dist * speed
        vy = dy / dist * speed
        return Triple(x + vx.toInt(), y + vy.toInt(), false)
    }

    private fun randAway(x: Int, y: Int, b: WalkBounds, minDist: Int): Pair<Int, Int> {
        val spanX = (b.maxX - b.minX).coerceAtLeast(0)
        val spanY = (b.maxY - b.minY).coerceAtLeast(0)
        repeat(8) {
            val nx = b.minX + if (spanX == 0) 0 else Random.nextInt(spanX + 1)
            val ny = b.minY + if (spanY == 0) 0 else Random.nextInt(spanY + 1)
            if (hypot((nx - x).toFloat(), (ny - y).toFloat()) >= minDist) return nx to ny
        }
        return b.minX + spanX / 2 to b.minY + spanY / 2
    }

    private fun finish(x: Int, y: Int, nap: Boolean): Triple<Int, Int, Result> {
        active = false
        return Triple(x, y, if (nap) Result.NAP else Result.IDLE)
    }
}

internal data class WalkBounds(val minX: Int, val maxX: Int, val minY: Int, val maxY: Int)
