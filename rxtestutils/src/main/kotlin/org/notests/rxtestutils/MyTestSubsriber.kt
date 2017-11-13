package org.notests.rxtestutils

import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.SerialDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.TestScheduler
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.*
import java.util.concurrent.TimeUnit.MILLISECONDS

/** Created by juraj on 12/11/2017. */

class MyTestSubscriber<T>(private val scheduler: TestScheduler) : Observer<T> {
    val disposable = SerialDisposable()
    override fun onSubscribe(d: Disposable) {
        disposable.replace(d)
    }

    override fun onComplete() {
        values += Recorded(scheduler.now(MILLISECONDS), Event.Completed as Event<T>)
    }

    private var values: List<Recorded<Event<T>>> = emptyList()

    override fun onError(e: Throwable) {
        values += Recorded(scheduler.now(MILLISECONDS), Event.Error(e ?: Error("Unknown")))
    }

    override fun onNext(t: T) {
        values += Recorded(scheduler.now(MILLISECONDS), Event.Next(t))
    }

    fun events() = values

    fun dispose() {
        disposable.dispose()
    }
}
