package org.notests.rxfeedback

private typealias Mutate<State> = (State) -> Unit

internal class AsyncSynchronized<State>(private val state: State) {

    private val queue = mutableListOf<Mutate<State>>()

    fun enqueueOrExecuteAll(mutate: (State) -> Unit) {
        var executeMutation = enqueue(mutate) ?: return
        do {
            executeMutation(state)
            val nextExecuteMutation = dequeue() ?: return
            executeMutation = nextExecuteMutation
        } while (true)
    }

    private fun enqueue(mutate: Mutate<State>): Mutate<State>? {
        synchronized(this) {
            val wasEmpty = queue.isEmpty()
            queue.add(mutate)
            return if (wasEmpty) {
                mutate
            } else null
        }
    }

    private fun dequeue(): Mutate<State>? {
        synchronized(this) {
            if (queue.isNotEmpty()) {
                queue.removeAt(0)
            }
            return queue.firstOrNull()
        }
    }
}