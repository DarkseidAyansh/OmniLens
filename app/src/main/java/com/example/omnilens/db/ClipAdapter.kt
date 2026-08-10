package com.example.omnilens.db

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.omnilens.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClipAdapter(
    private val onShareClick: (Data) -> Unit,
    private val onEditClick: (Data) -> Unit
) : RecyclerView.Adapter<ClipAdapter.ClipViewHolder>() {

    private var clips = emptyList<Data>()

    class ClipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appName: TextView = itemView.findViewById(R.id.tv_app_name)
        val clipText: TextView = itemView.findViewById(R.id.tv_clip_text)
        val timestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        val clipImage: ImageView = itemView.findViewById(R.id.iv_clip_image)
        val btnShare: Button = itemView.findViewById(R.id.btn_share)
        val btnEdit: Button = itemView.findViewById(R.id.btn_edit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_clip, parent, false)
        return ClipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClipViewHolder, position: Int) {
        val current = clips[position]

        holder.appName.text = current.sourceApp

        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        holder.timestamp.text = sdf.format(Date(current.timestamp))

        if (current.imagePath != null) {
            holder.clipText.visibility = View.GONE
            holder.clipImage.visibility = View.VISIBLE

            val imageFile = File(current.imagePath)
            if (imageFile.exists()) {
                holder.clipImage.setImageURI(Uri.fromFile(imageFile))
            }

            holder.btnEdit.text = "View"

        } else {
            holder.clipImage.visibility = View.GONE
            holder.clipText.visibility = View.VISIBLE
            holder.clipText.text = current.text

            holder.btnEdit.text = "Edit"
        }
        holder.btnShare.setOnClickListener { onShareClick(current) }
        holder.btnEdit.setOnClickListener { onEditClick(current) }
    }

    override fun getItemCount() = clips.size

    fun setData(newClips: List<Data>) {
        this.clips = newClips
        notifyDataSetChanged()
    }

    fun getClipAt(position: Int): Data {
        return clips[position]
    }
}