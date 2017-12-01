package org.notests.rxfeedbackexample

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import org.notests.rxfeedbackexample.MyListAdapter.ViewHolder
import org.notests.rxfeedbackexample.counter.Counter
import org.notests.rxfeedbackexample.play_catch.PlayCatch


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.title = "Examples"

        recyclerview.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = MyListAdapter(Item.values().toList(), {
                when (it) {
                    Item.counter -> startActivity(Intent(this@MainActivity, Counter::class.java))
                    Item.playCatch -> startActivity(Intent(this@MainActivity, PlayCatch::class.java))
                }
            })
        }
    }
}

enum class Item(val displayName: String) {
    counter("Counter"), playCatch("Play Catch")
}

class MyListAdapter(private val items: List<Item>, private val onClick: (Item) -> (Unit)) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_main, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.textView.text = item.displayName
        holder.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        val root: RelativeLayout
            get() = v.findViewById(R.id.item_root)
        val textView: TextView
            get() = v.findViewById(R.id.item_name)
    }
}
