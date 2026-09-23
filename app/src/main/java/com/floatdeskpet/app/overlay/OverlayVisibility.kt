package com.floatdeskpet.app.overlay

object OverlayVisibility {
    @Volatile
    var homeFront: Boolean = false

    fun shouldShow(userVisible: Boolean, homeFront: Boolean = this.homeFront): Boolean {
        return userVisible && !homeFront
    }
}
