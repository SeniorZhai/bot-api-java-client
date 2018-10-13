package one.mixin.example

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.addAddress
import kotlinx.android.synthetic.main.activity_main.asset_address_id
import kotlinx.android.synthetic.main.activity_main.asset_content
import kotlinx.android.synthetic.main.activity_main.content
import kotlinx.android.synthetic.main.activity_main.create
import kotlinx.android.synthetic.main.activity_main.create_pin
import kotlinx.android.synthetic.main.activity_main.getDeposit
import kotlinx.android.synthetic.main.activity_main.qr_code
import kotlinx.android.synthetic.main.activity_main.verfiy
import kotlinx.android.synthetic.main.activity_main.withdrawal
import one.mixin.bot.encryptPin
import one.mixin.bot.generateRSAKeyPair
import one.mixin.bot.getPrivateKeyPem
import one.mixin.bot.getPublicKey
import one.mixin.bot.getRSAPrivateKeyFromString
import one.mixin.bot.rsaDecrypt
import one.mixin.example.SecretKey.Account.PREF_USER
import one.mixin.example.SecretKey.Account.PREF_USER_ADDRESS_ID
import one.mixin.example.SecretKey.Account.PREF_USER_PRIVATE_KEY
import one.mixin.lib.HttpClient
import one.mixin.lib.HttpClient.TokenInfo
import one.mixin.lib.util.Base64
import one.mixin.lib.vo.AccountRequest
import one.mixin.lib.vo.AddressesRequest
import one.mixin.lib.vo.PinRequest
import one.mixin.lib.vo.User
import one.mixin.lib.vo.WithdrawalRequest
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.dip
import org.jetbrains.anko.toast
import java.security.KeyPair
import java.util.Random
import java.util.UUID

class MainActivity : AppCompatActivity() {

  private val client: HttpClient = HttpClient(SecretKey.USER_ID, SecretKey.SESSION_ID,
      SecretKey.PRIVATE_KEY)

  private var currentUser: User? = null
    set(value) {
      field = value
      if (value != null) {
        userIterator = AndroidPinIterator(this, value.userId)
        // RSA private key
        val privateKey = getRSAPrivateKeyFromString(userPrivateKey!!)
        // decrypt -> get AES Key
        userAesKey = rsaDecrypt(privateKey, currentUser!!.sessionId, currentUser!!.pinToken)
      }
    }

  // user pin iterator
  private var userIterator: AndroidPinIterator? = null
  // user rsa private key
  private var userPrivateKey: String? = null
  // user aes key
  private var userAesKey: String? = null

  @SuppressLint("SetTextI18n", "CommitPrefEdits")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    loadOldUser()
    create.setOnClickListener {
      client.clearUserInfo()
      clearAddress()
      val sessionKey = generateRSAKeyPair()
      val sessionSecret = Base64.encodeBytes(sessionKey.getPublicKey())
      client.userService.createUsersRx(
          AccountRequest("User${Random().nextInt(100)}", sessionSecret))
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread()).subscribe({ response ->
            if (response.isSuccess) {
              content.text = "${response.data}\n$userPrivateKey"
              saveUser(response.data!!, sessionKey)
            } else {
              content.text = "${response.error}"
            }
          }, {

          })
    }
    create_pin.setOnClickListener {
      if (currentUser == null || userIterator == null || userAesKey == null) {
        toast("no currentUser")
      } else {
        client.userService.createPinRx(
            PinRequest(encryptPin(userIterator!!, userAesKey!!, "131416")!!))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).subscribe({
              if (it.isSuccess) {
                toast("update success")
              } else {
                toast("update failure")
              }
            }, {

            })
      }
    }

    verfiy.setOnClickListener {
      if (currentUser == null || userIterator == null || userAesKey == null) {
        toast("no currentUser")
      } else {
        client.userService.pinVerifyRx(
            PinRequest(encryptPin(userIterator!!, userAesKey!!, "131416")!!))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).subscribe({
              if (it.isSuccess) {
                toast("Verify success")
              } else {
                toast("Verify failure")
              }
            }, {})
      }
    }

    getDeposit.setOnClickListener {
      // CNB
      client.assetService.getDepositRx("965e5c6e-434c-3fa9-b780-c50f43cd955c")
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread()).subscribe({
            if (it.isSuccess) {
              QRCodeUtil.createQRCodeBitmap(it.data!!.publicKey, dip(180), dip(180)).let {
                qr_code.setImageBitmap(it)
              }
              asset_content.text = "${it.data!!.balance} CNB"
            } else {
              toast("get deposit failure")
            }
          }, {

          })
    }

    addAddress.setOnClickListener {
      client.assetService.createAddressesRx(AddressesRequest(
          "965e5c6e-434c-3fa9-b780-c50f43cd955c",
          "0x45315C1Fd776AF95898C77829f027AFc578f9C2B",
          "CNB address",
          encryptPin(userIterator!!, userAesKey!!, "131416")!!))
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread()).subscribe({
            if (it.isSuccess) {
              toast("add success")
              saveAddress(it.data!!.addressId)
              asset_address_id.text = it.data!!.addressId
            } else {
              toast("add failure")
            }
          }, {

          })
    }
    withdrawal.setOnClickListener {
      val addressId = defaultSharedPreferences.getString(PREF_USER_ADDRESS_ID, null)
      if (addressId == null) {
        toast("Please add address")
      } else {
        client.assetService.withdrawalsRx(WithdrawalRequest(
            addressId,
            "4.9",
            encryptPin(userIterator!!, userAesKey!!, "131416")!!,
            UUID.randomUUID().toString(),
            "memo"
        )).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).subscribe({
              if (it.isSuccess) {
                toast("withdrawal success")
              } else {
                toast("withdrawal failure")
              }
            }, {

            })
      }
    }
  }

  private fun loadOldUser() {
    val userGson = defaultSharedPreferences.getString(PREF_USER,
        null)
    userPrivateKey = defaultSharedPreferences.getString(PREF_USER_PRIVATE_KEY, null)
    content.text = "$userGson\n$userPrivateKey"

    userGson?.let {
      currentUser = Gson().fromJson(userGson, User::class.java)
      client.setUserInfo(TokenInfo(currentUser!!.userId, currentUser!!.sessionId, userPrivateKey!!))
    }
    showAddress()
  }

  private fun saveAddress(address: String) {
    defaultSharedPreferences.edit().putString(PREF_USER_ADDRESS_ID,
        address).apply()
  }

  private fun showAddress() {
    defaultSharedPreferences.getString(PREF_USER_ADDRESS_ID, null).let {
      asset_address_id.text = it
    }
  }

  private fun clearAddress() {
    defaultSharedPreferences.edit().putString(PREF_USER_ADDRESS_ID, null).apply()
  }

  private fun saveUser(user: User, sessionKey: KeyPair) {
    currentUser = user
    userPrivateKey = sessionKey.getPrivateKeyPem()
    defaultSharedPreferences.edit().putString(PREF_USER,
        Gson().toJson(user)).apply()
    defaultSharedPreferences.edit().putString(PREF_USER_PRIVATE_KEY,
        sessionKey.getPrivateKeyPem()).apply()
    client.setUserInfo(
        TokenInfo(currentUser!!.userId, currentUser!!.sessionId, userPrivateKey!!))
  }

  private val botIterator by lazy {
    AndroidPinIterator(this, "bot")
  }
}
