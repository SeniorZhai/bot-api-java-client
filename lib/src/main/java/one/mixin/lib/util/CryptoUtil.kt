@file:JvmName("CryptoUtils")
package one.mixin.lib.util

import org.spongycastle.asn1.pkcs.PrivateKeyInfo
import org.spongycastle.util.io.pem.PemObject
import org.spongycastle.util.io.pem.PemWriter
import java.io.StringWriter
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource.PSpecified
import javax.crypto.spec.SecretKeySpec

fun generateRSAKeyPair(keyLength: Int = 1024): KeyPair {
  val kpg = KeyPairGenerator.getInstance("RSA")
  kpg.initialize(keyLength)
  return kpg.genKeyPair()
}

inline fun KeyPair.getPublicKey(): ByteArray {
  return public.encoded
}

fun getRSAPrivateKeyFromString(privateKeyPEM: String): PrivateKey {
  val striped = stripRsaPrivateKeyHeaders(privateKeyPEM)
  val keySpec = PKCS8EncodedKeySpec(Base64.decode(striped))
  val kf = KeyFactory.getInstance("RSA")
  return kf.generatePrivate(keySpec)
}

private fun stripRsaPrivateKeyHeaders(privatePem: String): String {
  val strippedKey = StringBuilder()
  val lines = privatePem.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
  lines.filter { line ->
    !line.contains("BEGIN RSA PRIVATE KEY") &&
        !line.contains("END RSA PRIVATE KEY") && !line.trim { it <= ' ' }.isEmpty()
  }
      .forEach { line -> strippedKey.append(line.trim { it <= ' ' }) }
  return strippedKey.toString().trim { it <= ' ' }
}

private val HEX_CHARS by lazy { "0123456789abcdef".toCharArray() }
fun ByteArray.toHex(): String {
  val result = StringBuffer()

  forEach {
    val octet = it.toInt()
    val firstIndex = (octet and 0xF0).ushr(4)
    val secondIndex = octet and 0x0F
    result.append(HEX_CHARS[firstIndex])
    result.append(HEX_CHARS[secondIndex])
  }

  return result.toString()
}


inline fun Long.toLeByteArray(): ByteArray {
  var num = this
  val result = ByteArray(8)
  for (i in (0..7)) {
    result[i] = (num and 0xffL).toByte()
    num = num shr 8
  }
  return result
}

fun KeyPair.getPrivateKeyPem(): String {
  val pkInfo = PrivateKeyInfo.getInstance(private.encoded)
  val encodable = pkInfo.parsePrivateKey()
  val primitive2 = encodable.toASN1Primitive()
  val privateKeyPKCS1 = primitive2.encoded

  val pemObject2 = PemObject("RSA PRIVATE KEY", privateKeyPKCS1)
  val stringWriter2 = StringWriter()
  val pemWriter2 = PemWriter(stringWriter2)
  pemWriter2.writeObject(pemObject2)
  pemWriter2.close()
  return stringWriter2.toString()
}

fun aesEncrypt(key: String, iterator: Long, code: String): String? {
  val keyByteArray = Base64.decode(key)
  val keySpec = SecretKeySpec(keyByteArray, "AES")
  val iv = ByteArray(16)
  SecureRandom().nextBytes(iv)
  val pinByte = code.toByteArray() + (System.currentTimeMillis() / 1000).toLeByteArray() + iterator.toLeByteArray()
  val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
  cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
  println("加密：" + Base64.encodeBytes(pinByte))
  val result = cipher.doFinal(pinByte)
  return Base64.encodeBytes(iv.plus(result))
}


fun rsaDecrypt(privateKey: PrivateKey, iv: String, pinToken: String): String {
  val deCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
  deCipher.init(Cipher.DECRYPT_MODE, privateKey, OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256,
      PSpecified(iv.toByteArray())))
  return Base64.encodeBytes(deCipher.doFinal(Base64.decode(pinToken)))
}
