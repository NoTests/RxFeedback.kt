package org.notests.rxfeedback

import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.notests.rxtestutils.*
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
        val res = scheduler.start {
            Observables.system("initial",
                    { _, newState: String -> newState },
                    scheduler,
                    emptyList())
        }

        assertEquals(listOf(
                next(200, "initial"),
                complete(200)
        ), res.events())
    }

    @Test
    fun testImmediateFeedbackLoop() {
        val res = scheduler.start {
            val feedback: Feedback<String, String> = {
                it.source.flatMap {
                    when (it) {
                        "initial" -> Observable.just("_a")
                                .delay(10, TimeUnit.MILLISECONDS, scheduler)
                        "initial_a" -> Observable.just("_b")
                        "initial_a_b" -> Observable.just("_c")
                        else -> Observable.never<String>()
                    }
                }
            }

            Observables.system("initial",
                    { oldState, append: String -> oldState + append },
                    scheduler,
                    listOf(feedback)
            )
        }

        assertEquals( listOf(
                next(200, "initial"),
                next(210, "initial_a"),
                next(210, "initial_a_b"),
                next(210, "initial_a_b_c")
                ), res.events()
        )
    }
}
