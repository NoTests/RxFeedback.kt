package org.notests.rxtestutils

import io.reactivex.schedulers.TestScheduler
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.TimeUnit.MILLISECONDS

/** Created by juraj on 12/11/2017. */

class MyTestSubscriber<T>(private val scheduler: TestScheduler) : Subscriber<T> {

    override fun onSubscribe(s: Subscription?) {
        // TODO
    }

    override fun onComplete() {
        values += Recorded(scheduler.now(MILLISECONDS), Event.Completed as Event<T>)
    }

    private var values: List<Recorded<Event<T>>> = emptyList()

    override fun onError(e: Throwable?) {
        values += Recorded(scheduler.now(MILLISECONDS), Event.Error(e ?: Error("Unknown")))
    }

    override fun onNext(t: T) {
        values += Recorded(scheduler.now(MILLISECONDS), Event.Next(t))
    }

    fun events() = values
}
