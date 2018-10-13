package one.mixin.lib.api

import io.reactivex.Observable
import one.mixin.lib.vo.Address
import one.mixin.lib.vo.AddressesRequest
import one.mixin.lib.vo.Asset
import one.mixin.lib.vo.Snapshot
import one.mixin.lib.vo.WithdrawalRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AssetService {
  @GET("assets/{id}")
  fun getDeposit(@Path("id") id: String): Call<MixinResponse<Asset>>

  @GET("assets/{id}")
  fun getDepositRx(@Path("id") id: String): Observable<MixinResponse<Asset>>

  @POST("withdrawals")
  fun withdrawals(@Body request: WithdrawalRequest): Call<MixinResponse<Snapshot>>

  @POST("withdrawals")
  fun withdrawalsRx(@Body request: WithdrawalRequest): Observable<MixinResponse<Snapshot>>

  @POST("addresses")
  fun createAddresses(@Body request: AddressesRequest):Call<MixinResponse<Address>>

  @POST("addresses")
  fun createAddressesRx(@Body request: AddressesRequest):Observable<MixinResponse<Address>>

}
