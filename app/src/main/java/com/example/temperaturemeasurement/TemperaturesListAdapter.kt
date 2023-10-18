package com.example.temperaturemeasurement

import android.content.SharedPreferences
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.temperaturemeasurement.UniqueSensorsDatabase.*
import com.google.android.material.card.MaterialCardView

//class TemperaturesListAdapter : ListAdapter<Temperatures, TemperaturesListAdapter.TemperaturesViewHolder>(
class TemperaturesListAdapter(uniqueSensorsViewModel: UniqueSensorsViewModel) : ListAdapter<UniqueSensors, TemperaturesListAdapter.TemperaturesViewHolder>(
    TemperaturesComparator()) {
    var AuniqueSensorsViewModel = uniqueSensorsViewModel
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemperaturesViewHolder {
        return TemperaturesViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: TemperaturesViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current.valueTemp, current.valueName, current.pathTemp, current.toRead)

        if (current.pathName != "ThermalService") {
            if (!current.toRead) {
                holder.materialCard.setCardBackgroundColor(Color.parseColor("#e74c3c"));
            }
            if (current.toRead) {
                holder.materialCard.setCardBackgroundColor(Color.parseColor("#c8f7c5"))
            }

            holder.itemView.setOnClickListener(View.OnClickListener {
                if (current.toRead) {
                    holder.materialCard.setCardBackgroundColor(Color.parseColor("#e74c3c"));
                    AuniqueSensorsViewModel.updateToRead(current.valueName, false)
                }
                if (!current.toRead) {
                    holder.materialCard.setCardBackgroundColor(Color.parseColor("#c8f7c5"))
                    AuniqueSensorsViewModel.updateToRead(current.valueName, true)
                }
            })
        }
        else {
            holder.materialCard.setCardBackgroundColor(Color.parseColor("#F1B051"));
        }
    }

    class TemperaturesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tempName: TextView = view.findViewById(R.id.tempName)
        val tempValue: TextView = view.findViewById(R.id.tempValue)
        val materialCard: MaterialCardView = view.findViewById(R.id.TempCard)

        fun bind(valueTemp: String, valueName: String, pathTemp: String, toRead: Boolean) {
            tempName.text = valueName
            tempValue.text = valueTemp.toString()
        }

        companion object {
            fun create(parent: ViewGroup): TemperaturesViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.temperatures_read_item, parent, false)
                return TemperaturesViewHolder(view)
            }
        }
    }

class TemperaturesComparator : DiffUtil.ItemCallback<UniqueSensors>() {
    override fun areItemsTheSame(oldItem: UniqueSensors, newItem: UniqueSensors): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: UniqueSensors, newItem: UniqueSensors): Boolean {
        return (oldItem.valueTemp == newItem.valueTemp) && (oldItem.valueName == newItem.valueName)
    }
}

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}