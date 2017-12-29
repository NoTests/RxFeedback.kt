[![](https://jitpack.io/v/NoTests/RxFeedback.kt.svg)](https://jitpack.io/#NoTests/RxFeedback.kt)

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
    * A lot of logic is just normal pure function that can be debugged using Xcode debugger, or just printing the commands.

* Can be applied on any level
    * [Entire system](https://kafka.apache.org/documentation/)
    * application (state is stored inside a database, CoreData, Firebase, Realm)
    * view controller (state is stored inside `system` operator)
    * inside feedback loop (another `system` operator inside feedback loop)
* Works awesome with dependency injection
* Testing
    * Reducer is a pure function, just call it and assert results
    * In case effects are being tested -> TestScheduler
* Can model circular dependencies
* Completely separates business logic from effects (Rx).
    * Business logic can be transpiled between platforms (ShiftJS, C++, J2ObjC)

