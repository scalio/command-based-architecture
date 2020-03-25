package io.scal.commandbasedarchitecture.sample_coroutine.ui.list

import android.app.Application
import androidx.lifecycle.LiveData
import io.scal.commandbasedarchitecture.CommandManager
import io.scal.commandbasedarchitecture.sample_coroutine.ui.viewmodel.ItemsBroadcastTypes
import io.scal.commandbasedarchitecture.sample_coroutine.ui.viewmodel.ItemsRootBroadcastViewModel

class BroadcastListViewModel(application: Application) : ListViewModel(application) {

    // should be provided as singleton. preferred way - by any DI
    private val rootBroadcastViewModel by lazy { ItemsRootBroadcastViewModel.instance }

    private val childViewModel by lazy {
        rootBroadcastViewModel.getChildViewModel(ItemsBroadcastTypes.AllItems)
    }

    override val dataState: LiveData<ListScreenState> = childViewModel.childLiveData
    override val commandManager: CommandManager<ListScreenState> = childViewModel.commandManager

    init {
        reload()
    }

    override fun reload() {
        childViewModel.reload()
    }

    override fun loadNextPage() {
        childViewModel.loadNextPage()
    }

    override fun removeFromFavorite(item: UIMainItem) {
        childViewModel.removeFromFavorite(item)
    }

    override fun addToFavorite(item: UIMainItem) {
        childViewModel.addToFavorite(item)
    }
}