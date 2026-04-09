package com.example.myapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemTutorialPageBinding

class TutorialPagerAdapter(
    private val pages: List<TutorialPage>
) : RecyclerView.Adapter<TutorialPagerAdapter.VH>() {

    // Helper class now holds the Binding, not just the View
    class VH(val binding: ItemTutorialPageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // Use ViewBinding inflation
        val binding = ItemTutorialPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val page = pages[position]

        // Access views directly via binding (CamelCase IDs)
        with(holder.binding) {
            img.setImageResource(page.imageRes)
            tvTitle.text = page.title
            tvDesc.text = page.desc
        }
    }

    override fun getItemCount(): Int = pages.size
}