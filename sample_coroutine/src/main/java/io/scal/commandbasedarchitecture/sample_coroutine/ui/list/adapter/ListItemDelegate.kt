package io.scal.commandbasedarchitecture.sample_coroutine.ui.list.adapter

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.scal.commandbasedarchitecture.sample_coroutine.R
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.adapter.AdapterDelegateByClass
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.ListViewModel
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.UIMainItem

internal class ListItemDelegate(
    private val viewModel: ListViewModel,
    private val onItemDetailsClick: (UIMainItem) -> Unit
) :
    AdapterDelegateByClass<UIMainItem, UIItem>(UIMainItem::class) {

    override val layoutId: Int = R.layout.item_main_adapter

    override fun isForViewType(items: List<UIItem>, position: Int): Boolean =
        items[position] is UIMainItem

    override fun onBindViewHolder(
        items: List<UIItem>,
        position: Int,
        viewHolder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val item = items[position] as UIMainItem

        viewHolder.itemView.setOnClickListener { onItemDetailsClick(item) }
        viewHolder.itemView.findViewById<TextView>(R.id.tvTitle).text = item.title

        if (item.favoriteState.favorite) {
            viewHolder.itemView.findViewById<Button>(R.id.addToFavorite).visibility = View.GONE
            viewHolder.itemView.findViewById<Button>(R.id.removeFromFavorite)
                .apply {
                    visibility = View.VISIBLE
                    setOnClickListener { viewModel.removeFromFavorite(item) }
                }
        } else {
            viewHolder.itemView.findViewById<Button>(R.id.removeFromFavorite).visibility = View.GONE
            viewHolder.itemView.findViewById<Button>(R.id.addToFavorite)
                .apply {
                    visibility = View.VISIBLE
                    setOnClickListener { viewModel.addToFavorite(item) }
                }
        }
    }
}