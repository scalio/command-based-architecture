package io.scal.commandbasedarchitecture.sample_coroutine.ui.list.adapter

import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.adapter.ProgressErrorItemDelegate
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.adapter.RecyclerViewAdapterDelegated
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.ListViewModel
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.UIMainItem

internal class ListAdapter(
    viewModel: ListViewModel,
    onItemDetailsClick: (UIMainItem) -> Unit
) : RecyclerViewAdapterDelegated<UIItem>(emptyList()) {

    init {
        addDelegate(ListItemDelegate(viewModel, onItemDetailsClick))
        addDelegate(ProgressErrorItemDelegate())
    }
}