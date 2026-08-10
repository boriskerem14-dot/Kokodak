package com.app.kokodak

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.kokodak.R

class SearchAdapter(
    private var results: List<SearchResult>
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    // ViewHolder съдържа референции към UI елементите на отделния елемент в списъка
    class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val urlTextView: TextView = itemView.findViewById(R.id.urlTextView)
        val snippetTextView: TextView = itemView.findViewById(R.id.snippetTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        // Надуваме layout-a за единичен елемент (item_search_result.xml)
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val item = results[position]

        // Попълваме данните от SearchResult обекта
        holder.titleTextView.text = item.title
        holder.urlTextView.text = item.url
        holder.snippetTextView.text = item.snippet
    }

    override fun getItemCount(): Int = results.size

    // Функция за обновяване на списъка, която викаш от MainActivity
    fun updateData(newResults: List<SearchResult>) {
        this.results = newResults
        notifyDataSetChanged()
    }
}