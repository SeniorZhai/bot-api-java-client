package one.mixin.example

import io.reactivex.Observable
import retrofit2.Call

fun <T> getObservable(call: Call<T>): Observable<T> {
  return Observable.just(call).map {
    call.execute().body()
  }
}
