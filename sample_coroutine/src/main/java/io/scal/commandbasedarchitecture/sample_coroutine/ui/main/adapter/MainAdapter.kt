package io.scal.commandbasedarchitecture.sample_coroutine.ui.main.adapter

import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.adapter.ProgressErrorItemDelegate
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.adapter.RecyclerViewAdapterDelegated
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.main.MainViewModel

internal class MainAdapter(
    viewModel: MainViewModel
) : RecyclerViewAdapterDelegated<UIItem>(emptyList()) {

    init {
        addDelegate(MainItemDelegate(viewModel))
        addDelegate(ProgressErrorItemDelegate())
    }
}