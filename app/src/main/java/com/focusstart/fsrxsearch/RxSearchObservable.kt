package com.focusstart.fsrxsearch

import android.widget.SearchView
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable


object RxSearchObservable {

    fun fromView(searchView: SearchView): Flowable<String> =
            Flowable.create({ emitter ->
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextChange(query: String?): Boolean {
                        emitter.onNext(query!!)
                        return false
                    }

                    override fun onQueryTextSubmit(submitQuery: String?): Boolean {
                        return false
                    }
                })
            }, BackpressureStrategy.BUFFER) // Рекомендованная стратегия в доках

}