package com.floatdeskpet.app.overlay

internal object SpeechBubbleLayout {
    fun place(
        petX: Int,
        petY: Int,
        petW: Int,
        petH: Int,
        bubbleW: Int,
        bubbleH: Int,
        screenW: Int,
        screenH: Int,
        padX: Int,
        padTop: Int,
        padBottom: Int,
        gap: Int,
    ): Pair<Int, Int> {
        val maxX = (screenW - bubbleW - padX).coerceAtLeast(padX)
        val maxY = (screenH - bubbleH - padBottom).coerceAtLeast(padTop)
        fun cx(x: Int) = x.coerceIn(padX, maxX)
        fun cy(y: Int) = y.coerceIn(padTop, maxY)

        val visLeft = petX.coerceAtLeast(0)
        val visTop = petY.coerceAtLeast(0)
        val visRight = (petX + petW).coerceAtMost(screenW)
        val visBottom = (petY + petH).coerceAtMost(screenH)
        val visW = (visRight - visLeft).coerceAtLeast(0)
        val visH = (visBottom - visTop).coerceAtLeast(0)
        if (visW == 0 || visH == 0) {
            return cx(padX) to cy(padTop)
        }
        val pcx = visLeft + visW / 2
        val sideY = visTop + (visH * 0.06f).toInt()
        val above = cx(pcx - bubbleW / 2) to cy(visTop - gap - bubbleH)
        val below = cx(pcx - bubbleW / 2) to cy(visBottom + gap)
        val right = cx(visRight + gap) to cy(sideY)
        val left = cx(visLeft - gap - bubbleW) to cy(sideY)

        val preferRight = pcx < screenW / 2
        val slots = if (preferRight) {
            arrayOf(above, right, left, below)
        } else {
            arrayOf(above, left, right, below)
        }
        for (slot in slots) {
            if (clearOf(slot.first, slot.second, bubbleW, bubbleH, visLeft, visTop, visW, visH, gap)) {
                return slot
            }
        }
        return slots.minBy { overlap(it.first, it.second, bubbleW, bubbleH, visLeft, visTop, visW, visH) }
    }

    private fun clearOf(
        bx: Int,
        by: Int,
        bw: Int,
        bh: Int,
        px: Int,
        py: Int,
        pw: Int,
        ph: Int,
        gap: Int,
    ): Boolean {
        if (overlap(bx, by, bw, bh, px, py, pw, ph) > 0) return false
        val separatedX = bx + bw <= px - gap || bx >= px + pw + gap
        val separatedY = by + bh <= py - gap || by >= py + ph + gap
        return separatedX || separatedY
    }

    private fun overlap(
        bx: Int,
        by: Int,
        bw: Int,
        bh: Int,
        px: Int,
        py: Int,
        pw: Int,
        ph: Int,
    ): Int {
        val w = minOf(bx + bw, px + pw) - maxOf(bx, px)
        val h = minOf(by + bh, py + ph) - maxOf(by, py)
        if (w <= 0 || h <= 0) return 0
        return w * h
    }
}
