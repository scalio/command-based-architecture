package io.scal.commandbasedarchitecture.sample_coroutine.ui.base.adapter

import androidx.recyclerview.widget.RecyclerView
import io.scal.commandbasedarchitecture.sample_coroutine.R
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIProgressErrorItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view.handleProgressErrorState

internal class ProgressErrorItemDelegate : AdapterDelegateByClass<UIProgressErrorItem, UIItem>(
    UIProgressErrorItem::class
) {

    override val layoutId: Int = R.layout.item_adapter_progress_error_footer

    override fun onBindItemViewHolder(
        item: UIProgressErrorItem,
        position: Int,
        viewHolder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        super.onBindItemViewHolder(item, position, viewHolder, payloads)

        item.handleProgressErrorState(viewHolder.itemView)
    }
}