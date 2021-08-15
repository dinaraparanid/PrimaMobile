package com.dinaraparanid.prima.utils.drawables

import com.dinaraparanid.prima.utils.Params
import top.defaults.drawabletoolbox.DrawableBuilder

/**
 * Custom divider with primary color
 * as app's current color
 */

internal class Divider private constructor() {
    internal companion object {
        @JvmField
        internal val instance = DrawableBuilder()
            .rectangle()
            .solidColor(Params.instance.primaryColor)
            .height(3)
            .width(1)
            .build()
    }
}