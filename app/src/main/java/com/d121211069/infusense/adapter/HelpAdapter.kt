package com.d121211069.infusense.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.d121211069.infusense.R
import com.d121211069.infusense.data.HelpItem
import com.d121211069.infusense.databinding.ItemHelpBinding

class HelpAdapter : ListAdapter<HelpItem, HelpAdapter.HelpViewHolder>(DiffCallback) {

    private var expandedPosition = -1

    inner class HelpViewHolder(private val binding: ItemHelpBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HelpItem, position: Int) {
            val isExpanded = position == expandedPosition
            with(binding) {
                helpQuestion.text = item.question
                helpAnswer.text = item.answer
                helpAnswer.visibility = if (isExpanded) View.VISIBLE else View.GONE
                arrowIcon.setImageResource(
                    if (isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
                )

                helpContainer.setOnClickListener {
                    val oldPos = expandedPosition
                    expandedPosition = if (isExpanded) -1 else position
                    notifyItemChanged(oldPos)
                    notifyItemChanged(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpViewHolder {
        val binding = ItemHelpBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HelpViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HelpViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<HelpItem>() {
            override fun areItemsTheSame(oldItem: HelpItem, newItem: HelpItem): Boolean {
                return oldItem.question == newItem.question
            }

            override fun areContentsTheSame(oldItem: HelpItem, newItem: HelpItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
