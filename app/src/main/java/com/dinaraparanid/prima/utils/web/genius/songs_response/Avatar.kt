package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose

data class Avatar(
    @Expose @JvmField val tiny: AvatarData,
    @Expose @JvmField val thumb: AvatarData,
    @Expose @JvmField val small: AvatarData,
    @Expose @JvmField val medium: AvatarData,
)