package com.warehouse.rfid.edge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

enum class TagAdapterMode { REGISTRATION, RECORD_ACTIVITY }

class TagAdapter(
    private var tags: List<TagRow>,
    private val mode: TagAdapterMode,
    private val locationProvider: () -> String? = { null },
    private val onTagClick: ((TagRow) -> Unit)? = null,
    private val onSelectionChanged: ((TagRow, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {

    private var selectionModeEnabled = false

    class TagViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)
        val vAccent: View = view.findViewById(R.id.vAccent)
        val tvEpc: TextView = view.findViewById(R.id.tvEpc)
        val tvCount: TextView = view.findViewById(R.id.tvCount)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val llTableRow: LinearLayout = view.findViewById(R.id.llTableRow)
        val tvColSku: TextView = view.findViewById(R.id.tvColSku)
        val tvColName: TextView = view.findViewById(R.id.tvColName)
        val tvColQty: TextView = view.findViewById(R.id.tvColQty)
        val tvColLocation: TextView = view.findViewById(R.id.tvColLocation)
        val tvReason: TextView = view.findViewById(R.id.tvReason)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tags[position]
        holder.tvEpc.text = tag.epc
        holder.tvCount.text = "x${tag.readCount}"
        holder.vAccent.setBackgroundColor(accentColorFor(holder, tag))

        if (mode == TagAdapterMode.REGISTRATION) {
            bindRegistration(holder, tag)
        } else {
            bindRecordActivity(holder, tag)
        }

        if (!tag.status.isNullOrEmpty()) {
            holder.tvStatus.visibility = View.VISIBLE
            holder.tvStatus.text = when (tag.status) {
                "ACCEPTED" -> "ACCEPTED"
                "UNKNOWN_EPC" -> "UNKNOWN"
                else -> "REJECTED"
            }
            val ctx = holder.itemView.context
            holder.tvStatus.setTextColor(
                when (tag.status) {
                    "ACCEPTED" -> ctx.getColor(R.color.status_available)
                    "UNKNOWN_EPC" -> ctx.getColor(R.color.status_unknown)
                    else -> ctx.getColor(R.color.status_rejected)
                }
            )
        } else {
            holder.tvStatus.visibility = View.GONE
        }

        if (!tag.reason.isNullOrEmpty()) {
            holder.tvReason.visibility = View.VISIBLE
            holder.tvReason.text = tag.reason
        } else {
            holder.tvReason.visibility = View.GONE
        }

        val canSelect = mode == TagAdapterMode.REGISTRATION && tag.status.isNullOrEmpty()
        if (selectionModeEnabled && canSelect) {
            holder.cbSelect.visibility = View.VISIBLE
            holder.cbSelect.setOnCheckedChangeListener(null)
            holder.cbSelect.isChecked = tag.isSelected
            holder.cbSelect.setOnCheckedChangeListener { _, checked ->
                tag.isSelected = checked
                onSelectionChanged?.invoke(tag, checked)
            }
        } else {
            holder.cbSelect.setOnCheckedChangeListener(null)
            holder.cbSelect.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if (mode == TagAdapterMode.REGISTRATION && canSelect) {
                if (selectionModeEnabled) {
                    holder.cbSelect.isChecked = !holder.cbSelect.isChecked
                } else {
                    onTagClick?.invoke(tag)
                }
            }
        }

        holder.itemView.setOnLongClickListener {
            if (canSelect) {
                tag.isSelected = true
                onSelectionChanged?.invoke(tag, true)
                true
            } else {
                false
            }
        }
    }

    fun setSelectionModeEnabled(enabled: Boolean) {
        if (selectionModeEnabled == enabled) return
        selectionModeEnabled = enabled
        notifyDataSetChanged()
    }

    fun isSelectionModeEnabled() = selectionModeEnabled

    private fun isUnknown(tag: TagRow) = tag.lookupState == LookupState.NOT_FOUND || tag.status == "UNKNOWN_EPC"

    private fun accentColorFor(holder: TagViewHolder, tag: TagRow): Int {
        val ctx = holder.itemView.context
        return when {
            tag.status == "ACCEPTED" -> ctx.getColor(R.color.status_available)
            tag.status == "UNKNOWN_EPC" -> ctx.getColor(R.color.status_unknown)
            !tag.status.isNullOrEmpty() -> ctx.getColor(R.color.status_rejected)
            tag.lookupState == LookupState.NOT_FOUND -> ctx.getColor(R.color.status_unknown)
            tag.lookupState == LookupState.FOUND -> ctx.getColor(R.color.brand_primary)
            mode == TagAdapterMode.REGISTRATION && !tag.productName.isNullOrEmpty() -> ctx.getColor(R.color.brand_primary)
            else -> ctx.getColor(R.color.divider)
        }
    }

    private fun bindRegistration(holder: TagViewHolder, tag: TagRow) {
        val ctx = holder.itemView.context
        if (!tag.productName.isNullOrEmpty()) {
            holder.tvColSku.text = tag.sku ?: "—"
            holder.tvColName.text = tag.productName
            holder.tvColQty.text = (tag.quantity ?: 1).toString()
            holder.tvColName.setTextColor(ctx.getColor(R.color.text_primary))
        } else {
            holder.tvColSku.text = "—"
            holder.tvColName.text = "Tap to register"
            holder.tvColQty.text = "—"
            holder.tvColName.setTextColor(ctx.getColor(R.color.brand_primary))
        }
        holder.tvColLocation.text = locationProvider()?.ifEmpty { null } ?: "—"
    }

    private fun bindRecordActivity(holder: TagViewHolder, tag: TagRow) {
        val ctx = holder.itemView.context
        when {
            isUnknown(tag) -> {
                holder.tvColSku.text = "—"
                holder.tvColName.text = "Unknown"
                holder.tvColQty.text = "—"
                holder.tvColLocation.text = "—"
                holder.tvColName.setTextColor(ctx.getColor(R.color.status_unknown))
            }
            tag.lookupState == LookupState.FOUND -> {
                holder.tvColSku.text = tag.sku ?: "—"
                holder.tvColName.text = tag.productName ?: "—"
                holder.tvColQty.text = (tag.quantity ?: 1).toString()
                holder.tvColLocation.text = tag.location ?: "—"
                holder.tvColName.setTextColor(ctx.getColor(R.color.text_primary))
            }
            else -> {
                holder.tvColSku.text = ""
                holder.tvColName.text = "Looking up..."
                holder.tvColQty.text = ""
                holder.tvColLocation.text = ""
                holder.tvColName.setTextColor(ctx.getColor(R.color.text_secondary))
            }
        }
    }

    override fun getItemCount() = tags.size

    fun updateData(newTags: List<TagRow>) {
        this.tags = newTags
        notifyDataSetChanged()
    }

    fun refreshLocationColumn() {
        if (mode == TagAdapterMode.REGISTRATION) notifyDataSetChanged()
    }
}
