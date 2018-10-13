package one.mixin.example

object SecretKey {
  const val USER_ID = "int put your bot user id"
  const val PIN_TOKEN = "int put your bot pin token"
  const val PIN = "int put your bot pin"
  const val SESSION_ID = "int put your bot session id"
  const val PRIVATE_KEY = "int put your bot private key"
  object Account {
    const val PREF_PIN_ITERATOR = "pref_pin_iterator"
    const val PREF_USER = "pref_user"
    const val PREF_USER_PRIVATE_KEY = "pref_user_key"
    const val PREF_USER_ADDRESS_ID = "pref_user_address_id"
    const val PREF_USER_ADDRESS = "pref_user_address"
  }
}