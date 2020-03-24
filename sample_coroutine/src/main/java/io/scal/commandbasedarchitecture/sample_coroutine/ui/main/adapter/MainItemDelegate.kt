package io.scal.commandbasedarchitecture.sample_coroutine.ui.main.adapter

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.scal.commandbasedarchitecture.sample_coroutine.R
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.adapter.AdapterDelegateByClass
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.main.MainViewModel
import io.scal.commandbasedarchitecture.sample_coroutine.ui.main.UIMainItem

internal class MainItemDelegate(private val viewModel: MainViewModel) :
    AdapterDelegateByClass<UIMainItem, UIItem>(UIMainItem::class) {

    override val layoutId: Int = R.layout.item_home_main_adapter

    override fun isForViewType(items: List<UIItem>, position: Int): Boolean =
        items[position] is UIMainItem

    override fun onBindViewHolder(
        items: List<UIItem>,
        position: Int,
        viewHolder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val item = items[position] as UIMainItem

        viewHolder.itemView.findViewById<TextView>(R.id.tvHome).text = item.title

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