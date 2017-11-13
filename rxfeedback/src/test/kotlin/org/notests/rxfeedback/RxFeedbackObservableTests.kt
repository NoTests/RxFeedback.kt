package org.notests.rxfeedback

import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.notests.rxtestutils.advanceTimeBy
import org.notests.rxtestutils.scheduleAt
import java.util.concurrent.TimeUnit

/**
 * Created by juraj on 12/11/2017.
 */
class RxFeedbackObservableTests {

    var scheduler = TestScheduler()

    @Before
    fun setup() {

    }

    // todo add testEventsAreArrivingOnCorrectScheduler - haven't investigated SerialDispatchQueueScheduler

    @Test
    fun testInitial() {
        var state = ""
        scheduler.scheduleAt(0, {
            val system = Observables.system("initial",
                    { _, newState: String -> newState },
                    scheduler,
                    emptyList())
            system.subscribe({ state = it })
        })
        scheduler.advanceTimeBy(1)

        assertEquals("initial", state)
    }

    @Test
    fun testImmediateFeedbackLoop() {
        var result = ArrayList<String>()

        scheduler.scheduleAt(0, {
            val feedback: Feedback<String, String> = {
                it.source.flatMap {
                    when (it) {
                        "initial" -> return@flatMap Observable.just("_a")
                    //TODO delay not working in tests?!!!     .delay(10, TimeUnit.MILLISECONDS)
                        "initial_a" -> return@flatMap Observable.just("_b")
                        "initial_a_b" -> return@flatMap Observable.just("_c")
                        else -> return@flatMap Observable.never<String>()
                    }
                }
            }

            val system = Observables.system("initial",
                    { oldState, append: String -> oldState + append },
                    scheduler,
                    listOf(feedback)
            )

            system
                    .take(4)
                    .timeout(500, TimeUnit.MILLISECONDS)
                    .onErrorResumeNext(Observable.empty<String>())
                    .subscribe({ result.add(it) })
            //TODO don't have to blocking with timeout. Tried this but .blockingIterable() is not working:
            //            result =  system
            //                    .take(4)
            //                    .timeout(500, TimeUnit.MILLISECONDS)
            //                    .onErrorResumeNext(Observable.empty<String>())
            //                    .blockingIterable()
            //                    .toList()

        })

        scheduler.advanceTimeBy(2000)

        assertEquals(listOf("initial", "initial_a", "initial_a_b", "initial_a_b_c"), result)
    }
}
