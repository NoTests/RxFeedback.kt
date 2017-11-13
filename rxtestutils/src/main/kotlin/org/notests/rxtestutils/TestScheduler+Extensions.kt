package org.notests.rxtestutils

import io.reactivex.Notification
import io.reactivex.Observable
import io.reactivex.rxkotlin.merge
import io.reactivex.schedulers.TestScheduler
import java.util.concurrent.TimeUnit.MILLISECONDS

/** Created by juraj on 12/11/2017. */

fun <T> TestScheduler.createColdObservable(vararg events: Recorded<Event<T>>): Observable<T> =
        events.map { (delay, value) ->
            Observable.timer(delay, MILLISECONDS, this)
                    .map {
                        when (value) {
                            is Event.Next -> Notification.createOnNext(value.value)
                            is Event.Completed -> Notification.createOnComplete()
                            is Event.Error -> Notification.createOnError(value.error)
                        }
                    }
        }.merge().dematerialize()

fun TestScheduler.scheduleAt(delay: Long, action: () -> Unit) =
        this.createWorker().schedule(action, delay, MILLISECONDS)

fun TestScheduler.advanceTimeBy(delay: Long) =
        this.advanceTimeBy(delay, MILLISECONDS)

fun <T> TestScheduler.createMyTestSubscriber() = MyTestSubscriber<T>(this)
