package org.notests.rxfeedbackexample.extensions

import android.view.View
import android.widget.TextView
import org.notests.rxfeedback.Optional

/**
 * Created by juraj on 16/12/2017.
 */

var View.isHidden: Boolean
    set(value) {
        if (value) {
            this.visibility = View.GONE
        } else this.visibility = View.VISIBLE
    }
    get() {
        return !this.isShown
    }

fun TextView.textOrHide(text: Optional<String>) =
        if (text is Optional.Some) {
            this.isHidden = false
            this.text = text.data
        } else {
            this.isHidden = true
        }
