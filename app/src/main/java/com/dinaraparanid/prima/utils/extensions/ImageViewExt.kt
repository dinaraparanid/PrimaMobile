package com.dinaraparanid.prima.utils.extensions

import carbon.widget.ImageView

/**
 * Sets shadow color for [ImageView]
 * @param color color to set
 */

internal fun ImageView.setShadowColor(color: Int) {
    setElevationShadowColor(color)
    outlineAmbientShadowColor = color
    outlineSpotShadowColor = color
}