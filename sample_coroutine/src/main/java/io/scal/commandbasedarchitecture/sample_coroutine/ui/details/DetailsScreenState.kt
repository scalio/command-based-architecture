package io.scal.commandbasedarchitecture.sample_coroutine.ui.details

import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.UIMainItem

data class DetailsScreenState(
    val item: UIMainItem? = null,
    val refreshStatus: UIItem? = null
)