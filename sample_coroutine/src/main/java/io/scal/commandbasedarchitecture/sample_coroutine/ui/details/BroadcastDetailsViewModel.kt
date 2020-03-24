package io.scal.commandbasedarchitecture.sample_coroutine.ui.details

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.UIMainItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.viewmodel.ItemsBroadcastTypes
import io.scal.commandbasedarchitecture.sample_coroutine.ui.viewmodel.ItemsRootBroadcastViewModel

class BroadcastDetailsViewModel(application: Application) : DetailsViewModel(application) {

    // should be provided as singleton. preferred way - by any DI
    private val rootBroadcastViewModel by lazy { ItemsRootBroadcastViewModel.instance }

    private val childViewModel by lazy {
        rootBroadcastViewModel.getChildViewModel(ItemsBroadcastTypes.OneItem(itemUid))
    }

    override val dataState: LiveData<DetailsScreenState> by lazy {
        childViewModel.childLiveData
            .map { DetailsScreenState(it.pageData?.itemsList?.firstOrNull(), it.refreshStatus) }
    }

    override fun reload() {
        childViewModel.reload()
    }

    override fun addToFavorite(item: UIMainItem) {
        childViewModel.addToFavorite(item)
    }

    override fun removeFromFavorite(item: UIMainItem) {
        childViewModel.removeFromFavorite(item)
    }
}