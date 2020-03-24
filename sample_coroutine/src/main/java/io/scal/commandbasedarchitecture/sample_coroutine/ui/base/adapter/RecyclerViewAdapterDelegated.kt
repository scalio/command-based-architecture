package io.scal.commandbasedarchitecture.sample_coroutine.ui.base.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates3.AdapterDelegate
import com.hannesdorfmann.adapterdelegates3.AdapterDelegatesManager
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIItem

abstract class RecyclerViewAdapterDelegated<Item : UIItem>(
    protected open var dataList: List<Item>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val delegatesManager = AdapterDelegatesManager<List<Item>>()

    protected fun addDelegate(delegate: AdapterDelegate<List<Item>>) {
        delegatesManager.addDelegate(delegate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        delegatesManager.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        delegatesManager.onBindViewHolder(dataList, position, holder)
    }

    override fun getItemViewType(position: Int): Int =
        delegatesManager.getItemViewType(dataList, position)

    override fun getItemCount(): Int = dataList.size

    fun setupData(newDataList: List<Item>?) {
        if (newDataList.isNullOrEmpty()) {
            releaseData()
        } else {
            updateData(newDataList)
        }
    }

    fun updateData(newDataList: List<Item>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = dataList[oldItemPosition]
                val newItem = newDataList[newItemPosition]
                return oldItem.key == newItem.key
            }

            override fun getOldListSize(): Int = dataList.size
            override fun getNewListSize(): Int = newDataList.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = dataList[oldItemPosition]
                val newItem = newDataList[newItemPosition]
                return oldItem == newItem
            }
        })
        dataList = newDataList
        diffResult.dispatchUpdatesTo(this)
    }

    fun releaseData() {
        updateData(emptyList())
    }
}