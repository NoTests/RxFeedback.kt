package org.notests.rxfeedbackexample.play_catch

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import kotlinx.android.synthetic.main.activity_play_catch.*
import org.notests.rxfeedback.*
import org.notests.rxfeedbackexample.R
import java.util.concurrent.TimeUnit

/**
 * Created by Juraj Begovac on 01/12/2017.
 */

enum class State {
    HumanHasIt, MachineHasIt
}

enum class Event {
    ThrowToMachine, ThrowToHuman
}

class PlayCatch : AppCompatActivity() {

    private var disposable: Disposable = Disposables.empty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_catch)

        supportActionBar?.title = "Play Catch"

        // UI is human feedback
        val bindUI = bind<State, Event> {
            val subscriptions = listOf<Disposable>(
                    it.source.map { it.myStateOfMind }.subscribe { my_label.text = it },
                    it.source.map { it.machineStateOfMind }.subscribe { machines_label.text = it },
                    it.source.map { !it.doIHaveTheBall }.subscribe { throw_the_ball_button.isHidden = it }
            )
            val events = listOf<Observable<Event>>(
                    RxView.clicks(throw_the_ball_button).map { Event.ThrowToMachine }
            )
            return@bind Bindings(subscriptions, events)
        }

        // NoUI, machine feedback
        val bindMachine = react<State, Unit, Event>(
                query = {
                    it.machinePitching
                },
                effects = {
                    Observable.timer(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                            .map { Event.ThrowToHuman }
                }
        )

        disposable = Observables.system(
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
                        bindMachine
                )
        )
                .subscribe()
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }
}

val State.myStateOfMind: String
    get() =
        when (this) {
            State.HumanHasIt -> "I have the 🏈"
            State.MachineHasIt -> "I'm ready, hit me"
        }

val State.doIHaveTheBall: Boolean
    get() = when (this) {
        State.HumanHasIt -> true
        State.MachineHasIt -> false
    }

val State.machineStateOfMind: String
    get() = when (this) {
        State.HumanHasIt -> "I'm ready, hit me"
        State.MachineHasIt -> "I have the 🏈"
    }

val State.machinePitching: Optional<Unit>
    get() = if (this == State.MachineHasIt) Optional.Some(Unit) else Optional.None()

var View.isHidden: Boolean
    set(value) {
        if (value) {
            this.visibility = View.GONE
        } else this.visibility = View.VISIBLE
    }
    get() {
        return !this.isShown
    }
