package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemTutorialPageBinding

class TutorialPagerAdapter(
    private val pages: List<TutorialPage>
) : RecyclerView.Adapter<TutorialPagerAdapter.VH>() {

    class VH(val root: View) : RecyclerView.ViewHolder(root) {
        val img: ImageView = root.findViewById(R.id.img)
        val tvTitle: TextView = root.findViewById(R.id.tvTitle)
        val tvDesc: TextView = root.findViewById(R.id.tvDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tutorial_page, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val page = pages[position]
        holder.img.setImageResource(page.imageRes)
        holder.tvTitle.text = page.title
        holder.tvDesc.text = page.desc
    }

    override fun getItemCount(): Int = pages.size
}

