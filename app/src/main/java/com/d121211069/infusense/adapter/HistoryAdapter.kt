package com.d121211069.infusense.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.d121211069.infusense.R
import com.d121211069.infusense.data.local.entity.HistoryEntity
import com.d121211069.infusense.databinding.ItemHistoryBinding
import com.d121211069.infusense.util.InfusStatus

class HistoryAdapter : ListAdapter<HistoryEntity, HistoryAdapter.HistoryViewHolder>(DIFF_CALLBACK) {

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryEntity) {
            with(binding) {
                textUser.text = item.user
                textDateTime.text = item.dateTime
                textLocation.text = item.location
                textDescription.text = item.description

                if (item.status == InfusStatus.KRITIS ||
                    item.status == InfusStatus.MINIMAL
                ) {
                    iconStatus.setImageResource(R.drawable.ic_infus)
                }

                if (item.status == InfusStatus.DRIP_ABNORMAL ||
                    item.status == InfusStatus.DRIP_STOP
                ) {
                    iconStatus.setImageResource(R.drawable.ic_water)
                }

                val colorRes = when (item.status) {
                    InfusStatus.KRITIS -> R.color.red
                    InfusStatus.MINIMAL -> R.color.orange
                    InfusStatus.DRIP_STOP -> R.color.red
                    InfusStatus.DRIP_ABNORMAL -> R.color.orange
                }
                iconStatus.background.setTint(ContextCompat.getColor(root.context, colorRes))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<HistoryEntity>() {
            override fun areItemsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: HistoryEntity,
                newItem: HistoryEntity
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}