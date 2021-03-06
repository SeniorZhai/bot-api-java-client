package one.mixin.lib.vo

import com.google.gson.annotations.SerializedName

data class PinRequest(
    val pin: String,
    @SerializedName("old_pin")
    val oldPin: String? = null)
