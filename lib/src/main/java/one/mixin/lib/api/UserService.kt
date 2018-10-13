package one.mixin.lib.api

import one.mixin.lib.vo.AccountRequest
import one.mixin.lib.vo.PinRequest
import one.mixin.lib.vo.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface UserService {
  @POST("users")
  fun createUsers(@Body request: AccountRequest): Call<MixinResponse<User>>

  @POST("pin/update")
  fun createPin(@Body request: PinRequest): Call<MixinResponse<User>>

  @POST("pin/verify")
  fun pinVerify(@Body request: PinRequest): Call<MixinResponse<User>>

}