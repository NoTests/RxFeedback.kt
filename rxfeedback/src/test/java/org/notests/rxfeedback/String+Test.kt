package org.notests.rxfeedback

/**
 * Created by juraj on 13/11/2017.
 */

val String.needsToAppendDot: Optional<Unit>
    get() =
        if (this == "initial" || this == "initial_." || this == "initial_._.") {
            Optional.Some(Unit)
        } else Optional.None()

val String.needsToAppend: Optional<String>
    get() =
        if (this == "initial") {
            Optional.Some("_a")
        } else if (this == "initial_a") {
            Optional.Some("_b")
        } else if (this == "initial_a_b") {
            Optional.Some("_c")
        } else {
            Optional.None<String>()
        }

val String.needsToAppendParallel: Set<String>
    get() =
        if (this.contains("_a") && this.contains("_b")) {
            setOf("_c")
        } else {
            var result = emptySet<String>()
            if (!this.contains("_a")) {
                result = result.plus("_a")
            }
            if (!this.contains("_b")) {
                result = result.plus("_b")
            }
            result
        }
