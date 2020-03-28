package io.scal.commandbasedarchitecture.sample_coroutine.ui.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.scal.commandbasedarchitecture.CommandManager
import io.scal.commandbasedarchitecture.broadcast.BaseBroadcastCommandViewModel
import io.scal.commandbasedarchitecture.broadcast.ChildCommandManager
import io.scal.commandbasedarchitecture.broadcast.DataState
import io.scal.commandbasedarchitecture.pagination.PageDataWithNextPageNumber
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.ListScreenState

class ItemsRootBroadcastViewModel(
    private val application: Application
) :
    BaseBroadcastCommandViewModel<ItemsBroadcastTypes, ItemsChildBroadcastViewModel>() {

    fun getChildViewModel(itemsBroadcastTypes: ItemsBroadcastTypes): ItemsChildBroadcastViewModel =
        getChildViewModel(itemsBroadcastTypes, itemsBroadcastTypes is ItemsBroadcastTypes.AllItems)

    override fun createChildViewModel(childKey: ItemsBroadcastTypes): ItemsChildBroadcastViewModel {
        val childLiveData = MutableLiveData(ListScreenState())
        return ItemsChildBroadcastViewModel(
            childKey,
            childLiveData,
            UsersChildCommandManager(childKey, commandManager, childLiveData),
            application
        )
    }

    companion object {
        lateinit var instance: ItemsRootBroadcastViewModel

        fun initIfNeeded(application: Application) {
            if (!::instance.isInitialized) {
                instance = ItemsRootBroadcastViewModel(application)
            }
        }
    }
}

private class UsersChildCommandManager(
    key: ItemsBroadcastTypes,
    commandManager: CommandManager<DataState<ItemsBroadcastTypes, ItemsChildBroadcastViewModel>>,
    childLiveData: LiveData<ListScreenState>
) : ChildCommandManager<ListScreenState, ItemsBroadcastTypes, ItemsChildBroadcastViewModel>
    (key, commandManager, childLiveData) {

    override fun updateViewModel(
        childViewModel: ItemsChildBroadcastViewModel,
        newState: ListScreenState
    ) {
        childViewModel.childLiveData as MutableLiveData
        val stateToUpdate = childViewModel.childLiveData.value!!
        val updatedState =
            applyNewDataToTypedState(stateToUpdate, newState, childViewModel.key == key)
        if (stateToUpdate != updatedState) {
            childViewModel.childLiveData.value = updatedState
        }
    }

    private fun applyNewDataToTypedState(
        stateToUpdate: ListScreenState,
        upToDateData: ListScreenState,
        fullUpdate: Boolean
    ): ListScreenState {
        val currentPageData = upToDateData.pageData
        return when {
            fullUpdate -> upToDateData
            null == currentPageData -> stateToUpdate
            else -> {
                val oldItems = stateToUpdate.pageData?.itemsList
                if (null == oldItems) {
                    stateToUpdate
                } else {
                    val updatedItems = oldItems.map { oldItem ->
                        currentPageData.itemsList
                            .firstOrNull { it.key == oldItem.key }
                            ?: oldItem
                    }
                    if (updatedItems == oldItems) stateToUpdate
                    else stateToUpdate.copy(
                        pageData = PageDataWithNextPageNumber(
                            updatedItems,
                            currentPageData.nextPageNumber
                        )
                    )
                }
            }
        }
    }
}