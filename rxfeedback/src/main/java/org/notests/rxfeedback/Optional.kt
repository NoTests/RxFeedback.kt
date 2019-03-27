package org.notests.rxfeedback

/**
 * Created by kzaher on 11/10/17.
 */

sealed class Optional<T> {
    data class Some<T>(val data: T): Optional<T>()
    class None<T>: Optional<T>()
}
