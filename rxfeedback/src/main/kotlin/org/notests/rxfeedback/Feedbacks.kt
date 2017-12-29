package org.notests.rxfeedback

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import org.notests.sharedsequence.Driver
import org.notests.sharedsequence.Signal
import org.notests.sharedsequence.asSignal
import org.notests.sharedsequence.empty

/**
 * State: State type of the system.
 * Query: Subset of state used to control the feedback loop.
 *
 * When query returns [some value][Optional.Some], that value is being passed into `effects` lambda to decide which effects should be performed.
 * In case new `query` is different from the previous one, new effects are calculated by using `effects` lambda and then performed.
 *
 * When `query` returns [Optional.None], feedback loops doesn't perform any effect.
 *
 * @param query Part of state that controls feedback loop.
 * @param areEqual Part of state that controls feedback loop.
 * @param effects Chooses which effects to perform for certain query result.
 * @return Feedback loop performing the effects.
 */
fun <State, Query, Event> react(
        query: (State) -> Optional<Query>,
        areEqual: (Query, Query) -> Boolean,
        effects: (Query) -> Observable<Event>
): (ObservableSchedulerContext<State>) -> Observable<Event> =
        { state ->
            state.source.map(query)
                    .distinctUntilChanged { lhs, rhs ->
                        when (lhs) {
                            is Optional.Some<Query> -> {
                                when (rhs) {
                                    is Optional.Some<Query> -> areEqual(lhs.data, rhs.data)
                                    is Optional.None<Query> -> false
                                }
                            }
                            is Optional.None<Query> -> {
                                when (rhs) {
                                    is Optional.Some<Query> -> false
                                    is Optional.None<Query> -> true
                                }
                            }
                        }
                    }
                    .switchMap { control: Optional<Query> ->
                        if (control !is Optional.Some<Query>) {
                            return@switchMap Observable.empty<Event>()
                        }

                        effects(control.data)
                                .enqueue(state.scheduler)
                    }
        }

/**
 * State: State type of the system.
 * Query: Subset of state used to control the feedback loop.
 *
 * When query returns [some value][Optional.Some], that value is being passed into `effects` lambda to decide which effects should be performed.
 * In case new `query` is different from the previous one, new effects are calculated by using `effects` lambda and then performed.
 *
 * When `query` returns [Optional.None], feedback loops doesn't perform any effect.
 *
 * @param query Part of state that controls feedback loop.
 * @param effects Chooses which effects to perform for certain query result.
 * @return Feedback loop performing the effects.
 */
fun <State, Query, Event> react(
        query: (State) -> Optional<Query>,
        effects: (Query) -> Observable<Event>
): (ObservableSchedulerContext<State>) -> Observable<Event> =
        react(query, { lhs, rhs -> lhs == rhs }, effects)

/**
 * State: State type of the system.
 * Query: Subset of state used to control the feedback loop.
 *
 * When query returns [some value][Optional.Some], that value is being passed into `effects` lambda to decide which effects should be performed.
 * In case new `query` is different from the previous one, new effects are calculated by using `effects` lambda and then performed.
 *
 * When `query` returns [Optional.None], feedback loops doesn't perform any effect.
 *
 * @param query Part of state that controls feedback loop.
 * @param areEqual Part of state that controls feedback loop.
 * @param effects Chooses which effects to perform for certain query result.
 * @return Feedback loop performing the effects.
 */
fun <State, Query, Event> reactSafe(
        query: (State) -> Optional<Query>,
        areEqual: (Query, Query) -> Boolean,
        effects: (Query) -> Signal<Event>
): (Driver<State>) -> Signal<Event> =
        { state ->
            val observableSchedulerContext = ObservableSchedulerContext<State>(
                    state.asObservable(),
                    Signal.scheduler
            )
            react(query, areEqual, { effects(it).asObservable() })(observableSchedulerContext)
                    .asSignal(Signal.empty())
        }

/**
 * State: State type of the system.
 * Query: Subset of state used to control the feedback loop.
 *
 * When query returns [some value][Optional.Some], that value is being passed into `effects` lambda to decide which effects should be performed.
 * In case new `query` is different from the previous one, new effects are calculated by using `effects` lambda and then performed.
 *
 * When `query` returns [Optional.None], feedback loops doesn't perform any effect.
 *
 * @param query Part of state that controls feedback loop.
 * @param effects Chooses which effects to perform for certain query result.
 * @return Feedback loop performing the effects.
 */
fun <State, Query, Event> reactSafe(
        query: (State) -> Optional<Query>,
        effects: (Query) -> Signal<Event>
): (Driver<State>) -> Signal<Event> =
        { state ->
            val observableSchedulerContext = ObservableSchedulerContext<State>(
                    state.asObservable(),
                    Signal.scheduler
            )
            react(query, { effects(it).asObservable() })(observableSchedulerContext)
                    .asSignal(Signal.empty())
        }

/**
 * State: State type of the system.
 * Query: Subset of state used to control the feedback loop.
 *
 * When `query` returns some set of values, each value is being passed into `effects` lambda to decide which effects should be performed.
 *
 * Effects are not interrupted for elements in the new `query` that were present in the `old` query.
 * Effects are cancelled for elements present in `old` query but not in `new` query.
 * In case new elements are present in `new` query (and not in `old` query) they are being passed to the `effects` lambda and resulting effects are being performed.
 *
 * @param query Part of state that controls feedback loop.
 * @param effects Chooses which effects to perform for certain query element.
 * @return Feedback loop performing the effects.
 */
fun <State, Query, Event> reactSet(
        query: (State) -> Set<Query>,
        effects: (Query) -> Observable<Event>
): (ObservableSchedulerContext<State>) -> Observable<Event> =
        { state ->
            val query = state.source.map(query)
                    .replay(1)
                    .refCount()

            val newQueries = Observable.zip(query, query.startWith(setOf<Query>()), BiFunction { current: Set<Query>, previous: Set<Query> -> current - previous })
            val asyncScheduler = state.scheduler
            newQueries.flatMap { controls: Set<Query> ->
                Observable.merge(controls.map { control ->
                    effects(control)
                            .enqueue(state.scheduler)
                            .takeUntilWithCompletedAsync(query.filter { !it.contains(control) }, state.scheduler)
                })
            }
        }

// This is important to avoid reentrancy issues. Completed event is only used for cleanup
fun <Element, O> Observable<Element>.takeUntilWithCompletedAsync(other: Observable<O>, scheduler: Scheduler): Observable<Element> {
    // this little piggy will delay completed event
    val completeAsSoonAsPossible = Observable.empty<Element>().observeOn(scheduler)
    return other
            .take(1)
            .map { _ -> completeAsSoonAsPossible }
            // this little piggy will ensure self is being run first
            .startWith(this)
            // this little piggy will ensure that new events are being blocked immediatelly
            .switchMap { it }
}

/**
 * State: State type of the system.
 * Query: Subset of state used to control the feedback loop.
 *
 * When `query` returns some set of values, each value is being passed into `effects` lambda to decide which effects should be performed.
 *
 * Effects are not interrupted for elements in the new `query` that were present in the `old` query.
 * Effects are cancelled for elements present in `old` query but not in `new` query.
 * In case new elements are present in `new` query (and not in `old` query) they are being passed to the `effects` lambda and resulting effects are being performed.
 *
 * @param query Part of state that controls feedback loop.
 * @param effects Chooses which effects to perform for certain query element.
 * @return Feedback loop performing the effects.
 */
fun <State, Query, Event> reactSetSafe(
        query: (State) -> Set<Query>,
        effects: (Query) -> Signal<Event>
): (Driver<State>) -> Signal<Event> =
        { state ->
            val observableSchedulerContext = ObservableSchedulerContext<State>(
                    state.asObservable(),
                    Signal.scheduler
            )
            reactSet(query, { effects(it).asObservable() })(observableSchedulerContext)
                    .asSignal(Signal.empty())
        }


fun <Element> Observable<Element>.enqueue(scheduler: Scheduler): Observable<Element> =
        this
                // observe on is here because results should be cancelable
                .observeOn(scheduler)
                // subscribe on is here because side-effects also need to be cancelable
                // (smooths out any glitches caused by start-cancel immediatelly)
                .subscribeOn(scheduler)


/**
 * Contains subscriptions and events.
 *
 * @param subscriptions map a system state to UI presentation.
 * @param events map events from UI to events of a given system.
 */
data class Bindings<Event>(val subscriptions: Iterable<Disposable>, val events: Iterable<Observable<Event>>) : Disposable {

    companion object {
        fun <Event> safe(subscriptions: Iterable<Disposable>, events: Iterable<Signal<Event>>): Bindings<Event> =
                Bindings(subscriptions, events.map { it.asObservable() })
    }

    override fun dispose() {
        for (subscription in subscriptions) {
            subscription.dispose()
        }
    }

    override fun isDisposed(): Boolean {
        return false
    }
}

/**
Bi-directional binding of a system State to external state machine and events from it.
 */
fun <State, Event> bind(bindings: (ObservableSchedulerContext<State>) -> (Bindings<Event>)): (ObservableSchedulerContext<State>) -> Observable<Event> =
        { state: ObservableSchedulerContext<State> ->
            Observable.using({
                bindings(state)
            }, { bindings: Bindings<Event> ->
                Observable.merge(bindings.events)
                        .enqueue(state.scheduler)
            }, { it.dispose() })
        }

/**
Bi-directional binding of a system State to external state machine and events from it.
 */
fun <State, Event> bindSafe(bindings: (Driver<State>) -> (Bindings<Event>)): (Driver<State>) -> Signal<Event> =
        { state: Driver<State> ->
            Observable.using({
                bindings(state)
            }, { bindings: Bindings<Event> ->
                Observable.merge(bindings.events)
            }, { it.dispose() })
                    .enqueue(Signal.scheduler)
                    .asSignal(Signal.empty<Event>())
        }

