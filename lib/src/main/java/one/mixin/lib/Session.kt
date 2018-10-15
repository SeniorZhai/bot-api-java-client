@file:JvmName("SessionUtil")
package one.mixin.lib

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import okhttp3.Request
import one.mixin.bot.arrayMapOf
import one.mixin.bot.bodyToString
import one.mixin.bot.cutOut
import one.mixin.bot.sha256
import one.mixin.lib.util.aesEncrypt
import one.mixin.lib.util.getRSAPrivateKeyFromString
import one.mixin.lib.util.toHex
import java.util.UUID

fun signToken(uId: String, sId: String, privateKey: String, request: Request): String {
  val key = getRSAPrivateKeyFromString(privateKey)
  val expire = System.currentTimeMillis() / 1000 + 1800
  val iat = System.currentTimeMillis() / 1000

  var content = "${request.method()}${request.url().cutOut()}"
  if (request.body() != null && request.body()!!.contentLength() > 0) {
    content += request.body()!!.bodyToString()
  }
  return Jwts.builder()
      .setClaims(arrayMapOf<String, Any>().apply {
        put(Claims.ID, UUID.randomUUID().toString())
        put(Claims.EXPIRATION, expire)
        put(Claims.ISSUED_AT, iat)
        put("uid", uId)
        put("sid", sId)
        put("sig", content.sha256().toHex())
        put("scp", "FULL")
      })
      .signWith(SignatureAlgorithm.RS512, key)
      .compact()
}


fun encryptPin(pinIterator: PinIterator, key: String, code: String?): String? {
  val pinCode = code ?: return null
  val based = aesEncrypt(key, pinIterator.getValue(), pinCode)
  pinIterator.increment()
  return based
}