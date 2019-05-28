package org.notests.rxfeedback

/**
 * Created by juraj on 13/11/2017.
 */

val String.needsToAppendDot: Unit?
    get() =
        if (this == "initial" || this == "initial_." || this == "initial_._.") {

        } else null

val String.needsToAppend: String?
    get() = when {
        this == "initial" -> "_a"
        this == "initial_a" -> "_b"
        this == "initial_a_b" -> "_c"
        else -> null
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
