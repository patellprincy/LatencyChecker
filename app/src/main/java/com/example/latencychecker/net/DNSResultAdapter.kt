package com.example.latencychecker.net

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.latencychecker.R

class DnsResultAdapter : RecyclerView.Adapter<DnsResultAdapter.VH>() {
    private var data: List<DnsBenchmark.Result> = emptyList()

    fun submit(new: List<DnsBenchmark.Result>) { data = new; notifyDataSetChanged() }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH {
        val row = LayoutInflater.from(p.context).inflate(R.layout.item_dns_result, p, false)
        return VH(row as ViewGroup)
    }

    override fun onBindViewHolder(h: VH, i: Int) = h.bind(data[i])

    override fun getItemCount(): Int = data.size

    class VH(private val root: ViewGroup) : RecyclerView.ViewHolder(root) {
        private val title: TextView = root.findViewById(R.id.title)
        private val subtitle: TextView = root.findViewById(R.id.subtitle)
        fun bind(r: DnsBenchmark.Result) {
            title.text = "${r.name}  •  ${if (r.medianMs >= 0) "${r.medianMs} ms" else "unreachable"}"
            subtitle.text = "${r.addr}  •  ${r.method}  •  samples=${r.samples.joinToString()}"
        }
    }
}
