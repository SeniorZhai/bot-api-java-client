package one.mixin.example

import android.content.Context
import one.mixin.example.SecretKey.Account.PREF_PIN_ITERATOR
import one.mixin.lib.PinIterator
import org.jetbrains.anko.defaultSharedPreferences

class AndroidPinIterator(val context: Context, val userid: String) : PinIterator {
  override fun getValue(): Long {
    return context.defaultSharedPreferences.getLong(
        "$PREF_PIN_ITERATOR$userid", 1)
  }

  override fun increment() {
    return context.defaultSharedPreferences.edit().putLong(
        "$PREF_PIN_ITERATOR$userid", getValue() + 1).apply()
  }

}
