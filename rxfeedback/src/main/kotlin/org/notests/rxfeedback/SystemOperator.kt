package org.notests.rxfeedback

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.subjects.ReplaySubject
import org.notests.sharedsequence.Driver
import org.notests.sharedsequence.Signal
import org.notests.sharedsequence.asDriver
import org.notests.sharedsequence.empty

/// Tuple of observable sequence and corresponding scheduler context on which that observable
/// sequence receives elements.
public data class ObservableSchedulerContext<Element>(
        val source: Observable<Element>,
        val scheduler: Scheduler
)

final class Observables {
    companion object {
    }
}

typealias Feedback<State, Event> = (ObservableSchedulerContext<State>) -> Observable<Event>
typealias SignalFeedback<State, Event> = (Driver<State>) -> Signal<Event>

/**
System simulation will be started upon subscription and stopped after subscription is disposed.

System state is represented as a `State` parameter.
Events are represented by `Event` parameter.

- parameter initialState: Initial state of the system.
- parameter accumulator: Calculates new system state from existing state and a transition event (system integrator, reducer).
- parameter feedback: Feedback loops that produce events depending on current system state.
- returns: Current state of the system.
 */
fun <State, Event> Observables.Companion.system(
        initialState: State,
        reduce: (State, Event) -> State,
        scheduler: Scheduler,
        scheduledFeedback: Iterable<Feedback<State, Event>>
): Observable<State> =
        Observable.defer {
            val replaySubject = ReplaySubject.createWithSize<State>(1)

            val events: Observable<Event> = Observable.merge(scheduledFeedback.map { feedback ->
                val state = ObservableSchedulerContext(replaySubject, scheduler)
                feedback(state)
            })

            events.scan(initialState, reduce)
                    .doOnNext { output ->
                        replaySubject.onNext(output)
                    }
                    .observeOn(scheduler)
        }!!

/**
System simulation will be started upon subscription and stopped after subscription is disposed.

System state is represented as a `State` parameter.
Events are represented by `Event` parameter.

- parameter initialState: Initial state of the system.
- parameter accumulator: Calculates new system state from existing state and a transition event (system integrator, reducer).
- parameter feedback: Feedback loops that produce events depending on current system state.
- returns: Current state of the system.
 */
fun <State, Event> Observables.system(
        initialState: State,
        reduce: (State, Event) -> State,
        scheduler: Scheduler,
        vararg scheduledFeedback: Feedback<State, Event>
): Observable<State> =
        Observables.system(initialState, reduce, scheduler, scheduledFeedback.toList())

/**
System simulation will be started upon subscription and stopped after subscription is disposed.

System state is represented as a `State` parameter.
Events are represented by `Event` parameter.

- parameter initialState: Initial state of the system.
- parameter accumulator: Calculates new system state from existing state and a transition event (system integrator, reducer).
- parameter feedback: Feedback loops that produce events depending on current system state.
- returns: Current state of the system.
 */
fun <State, Event> Driver.Companion.system(
        initialState: State,
        reduce: (State, Event) -> State,
        feedback: Iterable<SignalFeedback<State, Event>>
): Driver<State> =
        Observables.system(initialState, reduce, Driver.scheduler, feedback.map { signalFeedback ->
            { state: ObservableSchedulerContext<State> ->
                signalFeedback(state.source.asDriver { Driver.empty() })
                        .asObservable()
            }
        }).asDriver(Driver.empty())

/**
System simulation will be started upon subscription and stopped after subscription is disposed.

System state is represented as a `State` parameter.
Events are represented by `Event` parameter.

- parameter initialState: Initial state of the system.
- parameter accumulator: Calculates new system state from existing state and a transition event (system integrator, reducer).
- parameter feedback: Feedback loops that produce events depending on current system state.
- returns: Current state of the system.
 */
fun <State, Event> Driver.Companion.system(
        initialState: State,
        reduce: (State, Event) -> State,
        vararg feedback: SignalFeedback<State, Event>
): Driver<State> =
        Driver.system(initialState, reduce, feedback.toList())
