package com.dinaraparanid.prima.utils.drawables

import com.dinaraparanid.prima.utils.Params
import top.defaults.drawabletoolbox.DrawableBuilder

/**
 * Custom divider with primary color
 * as app's font color
 */

internal class FontDivider private constructor() {
    internal companion object {
        @JvmField
        internal val instance = DrawableBuilder()
            .rectangle()
            .solidColor(Params.instance.fontColor)
            .height(3)
            .width(1)
            .build()
    }
}