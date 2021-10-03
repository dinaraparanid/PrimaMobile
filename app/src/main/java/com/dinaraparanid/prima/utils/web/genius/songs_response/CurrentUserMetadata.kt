package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class CurrentUserMetadata(
    @Expose
    @JvmField
    val permissions: Array<String>,

    @Expose
    @JvmField
    @SerializedName("excluded_permissions")
    val excludedPermissions: Array<String>,

    @Expose
    @JvmField
    val interactions: Interactions,

    @Expose
    @JvmField
    val relationships: Relationships,

    @Expose
    @JvmField
    @SerializedName("iq_by_action")
    val iqByAction: IqByAction
)
