package one.mixin.lib

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import one.mixin.bot.ClientErrorException
import one.mixin.bot.ServerErrorException
import one.mixin.lib.Constants.API.URL
import one.mixin.lib.api.AssetService
import one.mixin.lib.api.UserService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit



class HttpClient(private val tokenInfo: TokenInfo) {

  constructor(uId: String, sId: String, privateKey: String) : this(TokenInfo(uId, sId, privateKey))

  class TokenInfo(var uId: String, var sId: String, var privateKey: String)

  private var userInfo: TokenInfo? = null

  fun setUserInfo(tokenInfo: TokenInfo) {
    userInfo = tokenInfo
  }

  fun clearUserInfo() {
    userInfo = null
  }

  private val okHttpClient: OkHttpClient by lazy {
    val builder = OkHttpClient.Builder()
    val logging = HttpLoggingInterceptor()
    logging.level = HttpLoggingInterceptor.Level.BODY
    builder.addNetworkInterceptor(logging)
    builder.connectTimeout(10, TimeUnit.SECONDS)
    builder.writeTimeout(10, TimeUnit.SECONDS)
    builder.readTimeout(10, TimeUnit.SECONDS)
    builder.pingInterval(15, TimeUnit.SECONDS)
    builder.retryOnConnectionFailure(false)

    builder.addInterceptor { chain ->
      val request = chain.request().newBuilder()
          .addHeader("User-Agent", Constants.UA)
          .addHeader("Accept-Language", Locale.getDefault().language)
          .addHeader("Authorization", "Bearer " +
              if (userInfo != null) {
                println("user:${userInfo!!.uId}")
                signToken(userInfo!!.uId, userInfo!!.sId, userInfo!!.privateKey, chain.request())
              } else {
                println("bot:${tokenInfo.uId}")
                signToken(tokenInfo.uId, tokenInfo.sId, tokenInfo.privateKey, chain.request())
              }).build()

      val response = try {
        chain.proceed(request)
      } catch (e: Exception) {
        if (e.message?.contains("502") == true) {
          throw ServerErrorException(502)
        } else throw e
      }

      if (!response.isSuccessful) {
        val code = response.code()
        if (code in 500..599) {
          throw ServerErrorException(code)
        } else if (code in 400..499) {
          throw ClientErrorException(code)
        }
      }
      return@addInterceptor response
    }
    builder.build()
  }


  private val retrofit: Retrofit by lazy {
    val builder = Retrofit.Builder()
        .baseUrl(URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
    builder.build()
  }


  val userService: UserService by lazy {
    retrofit.create(UserService::class.java)
  }

  val assetService: AssetService by lazy {
    retrofit.create(AssetService::class.java)
  }

}