package com.dinaraparanid.prima.utils.drawables

import com.dinaraparanid.prima.utils.Params
import top.defaults.drawabletoolbox.DrawableBuilder

internal class Marker private constructor() {
    internal companion object {
        internal var instance = DrawableBuilder()
            .rectangle()
            .solidColor(Params.instance.primaryColor)
            .height(10000)
            .width(50)
            .build()
            @JvmStatic
            @JvmName("getInstance")
            get
            private set

        @JvmStatic
        internal fun update() {
            instance = DrawableBuilder()
                .rectangle()
                .solidColor(Params.instance.primaryColor)
                .height(10000)
                .width(50)
                .build()
        }
    }
}