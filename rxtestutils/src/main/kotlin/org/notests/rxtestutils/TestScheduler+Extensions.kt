package org.notests.rxtestutils

import io.reactivex.Notification
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.merge
import io.reactivex.schedulers.TestScheduler
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

/** Created by juraj on 12/11/2017. */

object TestDefaults {
    val create = 100L // milliseconds
    val subscribe = 200L // milliseconds
    val dispose = 1000L // milliseconds
}


fun <T> TestScheduler.createColdObservable(vararg events: Recorded<Event<T>>): Observable<T> =
        events.map { (delay, value) ->
            Observable.timer(delay, MILLISECONDS, this)
                    .map {
                        when (value) {
                            is Event.Subscribed -> throw Exception("Unexpected event")
                            is Event.Next -> Notification.createOnNext(value.value)
                            is Event.Completed -> Notification.createOnComplete()
                            is Event.Error -> Notification.createOnError(value.error)
                        }
                    }
        }.merge().dematerialize()

fun TestScheduler.scheduleAt(delay: Long, timeUnit: TimeUnit = MILLISECONDS, action: () -> Unit) =
        this.createWorker().schedule(action, delay, MILLISECONDS)

fun TestScheduler.advanceTimeBy(delay: Long) =
        this.advanceTimeBy(delay, MILLISECONDS)

fun <T> TestScheduler.createMyTestSubscriber() = MyTestSubscriber<T>(this)

fun <T> TestScheduler.start(makeTestObservable: () -> Observable<T>): MyTestSubscriber<T> {
    var observable: Observable<T>? = null
    val testSubscriber = createMyTestSubscriber<T>()
    this.scheduleAt(TestDefaults.create) {
        observable = makeTestObservable()
    }
    this.scheduleAt(TestDefaults.subscribe) {
        observable!!.subscribe(testSubscriber)
    }
    this.scheduleAt(TestDefaults.dispose) {
        testSubscriber.dispose()
    }

    this.advanceTimeTo(200000, MILLISECONDS)

    return testSubscriber
}
