package com.dinaraparanid.prima.utils.drawables

import com.dinaraparanid.prima.utils.Params
import top.defaults.drawabletoolbox.DrawableBuilder

/**
 * Custom divider with primary color
 * as app's font color
 */

internal class FontDivider private constructor() {
    internal companion object {
        internal var instance = DrawableBuilder()
            .rectangle()
            .solidColor(Params.instance.fontColor)
            .height(3)
            .width(1)
            .build()
            @JvmStatic
            @JvmName("getInstance") get
            private set

        @JvmStatic
        internal fun update() {
            instance = DrawableBuilder()
                .rectangle()
                .solidColor(Params.instance.fontColor)
                .height(3)
                .width(1)
                .build()
        }
    }
}