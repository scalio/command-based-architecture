package io.scal.commandbasedarchitecture.sample_coroutine.ui.base.adapter

import androidx.recyclerview.widget.RecyclerView
import kotlin.reflect.KClass

internal abstract class AdapterDelegateByClass<DelegateItem : Any, ListItem>(private val dataClass: KClass<DelegateItem>) :
    AdapterDelegateListBase<ListItem>() {

    override fun isForViewType(items: List<ListItem>, position: Int): Boolean =
        dataClass.isInstance(items[position])

    override fun onBindViewHolder(
        items: List<ListItem>,
        position: Int,
        viewHolder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        @Suppress("UNCHECKED_CAST")
        onBindItemViewHolder(items[position] as DelegateItem, position, viewHolder, payloads)
    }

    protected open fun onBindItemViewHolder(
        item: DelegateItem,
        position: Int,
        viewHolder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {

    }
}