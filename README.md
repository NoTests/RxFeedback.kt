[![CircleCI](https://circleci.com/gh/NoTests/RxFeedback.kt.svg?style=shield&circle-token=727b4ab19d1007e8e5e1f3c633a499217a613687)](https://circleci.com/gh/NoTests/RxFeedback.kt) [![](https://jitpack.io/v/NoTests/RxFeedback.kt.svg)](https://jitpack.io/#NoTests/RxFeedback.kt)

#### RxFeedback 
Kotlin version of [RxFeedback](https://github.com/kzaher/RxFeedback)

[20min video pitch](https://academy.realm.io/posts/try-swift-nyc-2017-krunoslav-zaher-modern-rxswift-architectures/)

The simplest architecture for [RxJava](https://github.com/ReactiveX/RxJava)

<img src="https://github.com/kzaher/rxswiftcontent/raw/master/RxFeedback.png" width="502px" />

```kotlin
    typealias Feedback<State, Event> = (Observable<State>) -> Observable<Event>

    fun <State, Event> system(
            initialState: State,
            reduce: (State, Event) -> State,
            vararg feedback: Feedback<State, Event>
        ): Observable<State>
```

# Why

* Straightforward
    * if it's state -> State
    * if it's a way to modify state -> Event/Command
    * it it's an effect -> encode it into part of state and then design a feedback loop
* Declarative
    * System behavior is first declaratively specified and effects begin after subscribe is called => Compile time proof there are no "unhandled states"
* Debugging is easier
    * A lot of logic is just normal pure function that can be debugged using Android studio debugger, or just printing the commands.

* Can be applied on any level
    * [Entire system](https://kafka.apache.org/documentation/)
    * application (state is stored inside a database, Firebase, Realm)
    * Activity/Fragment/ViewModel(Android Architecture Component) (state is stored inside `system` operator)
    * inside feedback loop (another `system` operator inside feedback loop)
* Works awesome with dependency injection
* Testing
    * Reducer is a pure function, just call it and assert results
    * In case effects are being tested -> TestScheduler
* Can model circular dependencies
* Completely separates business logic from effects (Rx).
    * Business logic can be transpiled between platforms (ShiftJS, C++, J2ObjC)

# Examples

## Simple UI Feedback loop
```kotlin
Observables.system(initialState = 0,
                reduce = { state, event: Event ->
                    when (event) {
                        Event.Increment -> state + 1
                        Event.Decrement -> state - 1
                    }
                },
                scheduler = AndroidSchedulers.mainThread(),
                scheduledFeedback = listOf(
                        bind {
                            val subscriptions = listOf(
                                    it.source.map { it.toString() }.subscribe { label.text = it }
                            )
                            val events = listOf(
                                    RxView.clicks(plus).map { Event.Increment },
                                    RxView.clicks(minus).map { Event.Decrement }
                            )
                            return@bind Bindings(subscriptions, events)
                        }
		)
	)
```
<img src="https://github.com/JurajBegovac/rxfeedbackcontent/raw/master/Counter.gif" width="320px" />

## Play Catch

Simple automatic feedback loop.

```kotlin
Observables.system(
                initialState = State.HumanHasIt,
                reduce = { state, event: Event ->
                    when (event) {
                        Event.ThrowToMachine -> State.MachineHasIt
                        Event.ThrowToHuman -> State.HumanHasIt
                    }
                },
                scheduler = AndroidSchedulers.mainThread(),
                scheduledFeedback = listOf(
                        bindUI,
                        react<State, Unit, Event>(query = { it.machinePitching},
				effects = {
				    Observable.timer(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
					    .map { Event.ThrowToHuman }
					    }
			)
                )
	)
```
<img src="https://github.com/JurajBegovac/rxfeedbackcontent/raw/master/PlayCatch.gif" width="320px" />

## Paging
```kotlin
Driver.system(
                initialState = State.empty,
                reduce = { state: State, event: Event -> State.reduce(state, event) },
                feedback = listOf(bindUI,
                        reactSafe<State, String, Event>(
                                query = { it.loadNextPage },
                                effects = {
                                    repositoryService.getSearchRepositoriesResponse(it)
                                            .asSignal(onError = Signal.just(Result.Failure(GitHubServiceError.Offline) as SearchRepositoriesResponse))
                                            .map { Event.Response(it) }
                                }
                        )))
```

# How to include?
Add it in your root build.gradle at the end of repositories:
```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```
 Add the dependency
```
implementation 'com.github.NoTests:RxFeedback.kt:0.1.4'
```

