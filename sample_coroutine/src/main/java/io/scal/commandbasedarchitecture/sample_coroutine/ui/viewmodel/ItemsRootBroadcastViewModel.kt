package io.scal.commandbasedarchitecture.sample_coroutine.ui.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.scal.commandbasedarchitecture.CommandManager
import io.scal.commandbasedarchitecture.broadcast.BaseBroadcastCommandViewModel
import io.scal.commandbasedarchitecture.broadcast.ChildCommandManager
import io.scal.commandbasedarchitecture.broadcast.DataState
import io.scal.commandbasedarchitecture.model.applyNewDataToOtherState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.ListScreenState

class ItemsRootBroadcastViewModel(
    private val application: Application
) : BaseBroadcastCommandViewModel<ItemsBroadcastTypes, ListScreenState, ItemsChildBroadcastViewModel>() {

    fun getChildViewModel(itemsBroadcastTypes: ItemsBroadcastTypes): ItemsChildBroadcastViewModel =
        getChildViewModel(itemsBroadcastTypes, itemsBroadcastTypes is ItemsBroadcastTypes.AllItems)

    override fun createChildViewModel(
        childKey: ItemsBroadcastTypes,
        cachedChildState: MutableLiveData<ListScreenState>?
    ): ItemsChildBroadcastViewModel {
        val childLiveData = cachedChildState ?: MutableLiveData(ListScreenState())
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
    commandManager: CommandManager<DataState<ItemsBroadcastTypes, ListScreenState>>,
    childLiveData: LiveData<ListScreenState>
) : ChildCommandManager<ItemsBroadcastTypes, ListScreenState>(key, commandManager, childLiveData) {

    override fun updateState(
        stateToUpdateKey: ItemsBroadcastTypes,
        stateToUpdateLiveData: MutableLiveData<ListScreenState>,
        newState: ListScreenState
    ) {
        val stateToUpdate = stateToUpdateLiveData.value ?: return
        val updatedState =
            applyNewDataToOtherState(
                stateToUpdate,
                newState,
                stateToUpdateKey == key
            ) { newItem, oldItem -> newItem.key == oldItem.key }
        if (stateToUpdate != updatedState) {
            stateToUpdateLiveData.value = updatedState
        }
    }
}