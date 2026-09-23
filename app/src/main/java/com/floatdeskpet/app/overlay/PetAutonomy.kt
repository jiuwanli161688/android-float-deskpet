package com.floatdeskpet.app.overlay

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

internal class PetAutonomy {
    enum class Result { MOVE, IDLE, NAP }

    private enum class Kind { DIAGONAL, HOP, PAUSE_TURN, ARC, TWO_SEG, TARGET }

    private var kind = Kind.DIAGONAL
    private var until = 0L
    private var startedAt = 0L
    private var vx = 0f
    private var vy = 0f
    private var accX = 0f
    private var accY = 0f
    private var hopPhase = 0f
    private var hopAmp = 6f
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
    private var speedMul = 1f
    private var turning = false
    private var turnFromVx = 0f
    private var turnFromVy = 0f
    private var turnToVx = 0f
    private var turnToVy = 0f
    private var turnStart = 0L
    private var turnMs = 420L
    private var lastNow = 0L
    private var cruiseVx = 0f
    var active = false
        private set

    fun begin(x: Int, y: Int, b: WalkBounds, now: Long) {
        active = true
        accX = 0f
        accY = 0f
        pausing = false
        turned = false
        turning = false
        seg = 0
        speedMul = 0f
        startedAt = now
        pick(x, y, b, now)
    }

    fun cancel() {
        active = false
        turning = false
    }

    fun step(x: Int, y: Int, b: WalkBounds, now: Long): Triple<Int, Int, Result> {
        if (!active) return Triple(x, y, Result.IDLE)
        lastNow = now
        speedMul = envelope(now)
        tickTurn(now)
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
                accX += vx * speedMul
                val dx = accX.toInt()
                accX -= dx
                nx += dx
                if (nx <= b.minX) {
                    nx = b.minX
                    beginTurn(abs(vx), 0f, now, 360L)
                } else if (nx >= b.maxX) {
                    nx = b.maxX
                    beginTurn(-abs(vx), 0f, now, 360L)
                }
                hopPhase += 0.148f
                val lift = (1f - cos(hopPhase.toDouble()).toFloat()) * 0.5f * hopAmp *
                    (0.38f + 0.62f * speedMul)
                ny = (hopBaseY - lift.toInt()).coerceIn(b.minY, b.maxY)
            }
            Kind.PAUSE_TURN -> {
                if (now >= until) return finish(nx, ny, nap = false)
                if (pausing) {
                    if (now >= pauseAt) {
                        pausing = false
                        turned = true
                        val dir = if (abs(cruiseVx) < 0.2f) {
                            if (Random.nextBoolean()) 1f else -1f
                        } else {
                            -sign(cruiseVx)
                        }
                        val nvx = dir * (2.05f + Random.nextFloat() * 0.85f)
                        beginTurn(nvx, (Random.nextFloat() - 0.5f) * 1.1f, now, 520L)
                    }
                } else {
                    val moved = applyVel(nx, ny, b, bounce = true)
                    nx = moved.first
                    ny = moved.second
                    if (!turned && now >= pauseAt) {
                        pausing = true
                        pauseAt = now + (520L..980L).random()
                        beginTurn(0f, 0f, now, 380L)
                    }
                }
            }
            Kind.ARC -> {
                if (now >= until) return finish(nx, ny, nap = false)
                arcA += arcDa * (0.28f + 0.72f * speedMul)
                nx = (arcCx + cos(arcA) * arcR).toInt().coerceIn(b.minX, b.maxX)
                ny = (arcCy + sin(arcA) * arcR * 0.52f).toInt().coerceIn(b.minY, b.maxY)
            }
            Kind.TWO_SEG -> {
                if (now >= until) return finish(nx, ny, nap = false)
                val tx = if (seg == 0) wayX else destX
                val ty = if (seg == 0) wayY else destY
                val reached = seek(nx, ny, tx, ty, 2.7f)
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
                val reached = seek(nx, ny, destX, destY, 2.6f)
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
                val speed = 1.85f + Random.nextFloat() * 1.25f
                val ang = Random.nextFloat() * (Math.PI.toFloat() * 2f)
                vx = cos(ang) * speed
                vy = sin(ang) * speed * 0.62f
                if (abs(vx) < 0.7f) vx = if (vx >= 0f) 1.05f else -1.05f
                until = now + (7500L..14000L).random()
            }
            roll < 0.40f -> {
                kind = Kind.HOP
                vx = (if (Random.nextBoolean()) 1f else -1f) * (1.9f + Random.nextFloat() * 1.05f)
                vy = 0f
                hopPhase = 0f
                hopAmp = (5..9).random().toFloat()
                val amp = hopAmp.toInt()
                hopBaseY = if (b.maxY - b.minY < amp * 2) {
                    y.coerceIn(b.minY, b.maxY)
                } else {
                    y.coerceIn(b.minY + amp, b.maxY - amp)
                }
                until = now + (6500L..11500L).random()
            }
            roll < 0.56f -> {
                kind = Kind.PAUSE_TURN
                vx = (if (x > (b.minX + b.maxX) / 2) -1f else 1f) * (2.15f + Random.nextFloat() * 0.95f)
                cruiseVx = vx
                vy = (Random.nextFloat() - 0.5f) * 0.7f
                pauseAt = now + (2000L..3800L).random()
                pausing = false
                turned = false
                until = now + (8500L..14000L).random()
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
                arcDa = (if (Random.nextBoolean()) 1f else -1f) * (0.006f + Random.nextFloat() * 0.006f)
                until = now + (5500L..8500L).random()
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
        accX += vx * speedMul
        accY += vy * speedMul
        val dx = accX.toInt()
        val dy = accY.toInt()
        accX -= dx
        accY -= dy
        var nx = x + dx
        var ny = y + dy
        if (bounce) {
            if (nx <= b.minX) {
                nx = b.minX
                beginTurn(abs(vx).coerceAtLeast(0.8f), vy, lastNow, 340L)
            } else if (nx >= b.maxX) {
                nx = b.maxX
                beginTurn(-abs(vx).coerceAtLeast(0.8f), vy, lastNow, 340L)
            }
            if (ny <= b.minY) {
                ny = b.minY
                beginTurn(vx, abs(vy).coerceAtLeast(0.5f), lastNow, 340L)
            } else if (ny >= b.maxY) {
                ny = b.maxY
                beginTurn(vx, -abs(vy).coerceAtLeast(0.5f), lastNow, 340L)
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
        val arrive = (dist / 92f).coerceIn(0.22f, 1f)
        val spd = speed * speedMul * arrive
        if (dist <= speed + 2.2f) {
            vx = dx
            vy = dy
            return Triple(tx, ty, true)
        }
        vx = dx / dist * spd
        vy = dy / dist * spd
        accX += vx
        accY += vy
        val ix = accX.toInt()
        val iy = accY.toInt()
        accX -= ix
        accY -= iy
        return Triple(x + ix, y + iy, false)
    }

    private fun envelope(now: Long): Float {
        val elapsed = (now - startedAt).toFloat()
        val remain = (until - now).toFloat()
        val ein = if (elapsed < EASE_IN_MS) smooth(elapsed / EASE_IN_MS) else 1f
        val eout = when {
            remain < 0f -> 0f
            remain < EASE_OUT_MS -> smooth(remain / EASE_OUT_MS)
            else -> 1f
        }
        return (ein * eout).coerceIn(0.06f, 1f)
    }

    private fun beginTurn(nvx: Float, nvy: Float, now: Long, ms: Long) {
        turnFromVx = vx
        turnFromVy = vy
        turnToVx = nvx
        turnToVy = nvy
        turnStart = now
        turnMs = ms
        turning = true
    }

    private fun tickTurn(now: Long) {
        if (!turning) return
        val t = ((now - turnStart).toFloat() / turnMs.toFloat()).coerceIn(0f, 1f)
        val e = smooth(t)
        vx = turnFromVx + (turnToVx - turnFromVx) * e
        vy = turnFromVy + (turnToVy - turnFromVy) * e
        if (t >= 1f) turning = false
    }

    private fun smooth(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
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
        turning = false
        return Triple(x, y, if (nap) Result.NAP else Result.IDLE)
    }

    companion object {
        private const val EASE_IN_MS = 560f
        private const val EASE_OUT_MS = 720f
    }
}

internal data class WalkBounds(val minX: Int, val maxX: Int, val minY: Int, val maxY: Int)
