package io.scal.commandbasedarchitecture.broadcast

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.scal.commandbasedarchitecture.ActionCommand
import io.scal.commandbasedarchitecture.CommandManager
import io.scal.commandbasedarchitecture.CommandManagerImpl
import io.scal.commandbasedarchitecture.DataConvertCommandSameResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.lang.ref.WeakReference

interface ChildViewModel<ChildKey> {
    val key: ChildKey
}

data class DataState<ChildKey, ChildModel : ChildViewModel<ChildKey>>(
    val hardViewModels: Map<ChildKey, ChildModel>,
    val weakViewModels: List<WeakReference<ChildModel>>
) {

    val allActiveViewModels: List<ChildModel>
        get() = hardViewModels.values.plus(weakViewModels.mapNotNull { it.get() })
}

abstract class BaseBroadcastCommandViewModel<ChildKey, ChildModel : ChildViewModel<ChildKey>> {

    private val mutableDataState = MutableLiveData<DataState<ChildKey, ChildModel>>(DataState(emptyMap(), emptyList()))

    protected val commandManager: CommandManager<DataState<ChildKey, ChildModel>> by lazy { createCommandManager(mutableDataState) }
    protected val viewModelScope = CoroutineScope(Dispatchers.Main)

    protected open fun createCommandManager(mutableDataState: MutableLiveData<DataState<ChildKey, ChildModel>>): CommandManager<DataState<ChildKey, ChildModel>> =
        CommandManagerImpl(mutableDataState, viewModelScope)

    protected fun getChildViewModel(childKey: ChildKey, hardViewModel: Boolean): ChildModel {
        val currentDataState = mutableDataState.value!!
        val cachedViewModel = findChildViewModel(childKey, currentDataState)
        if (null != cachedViewModel) {
            return cachedViewModel
        }

        val newViewModel = createChildViewModel(childKey)

        val newDataState =
            if (hardViewModel)
                currentDataState.copy(
                    hardViewModels = currentDataState.hardViewModels.plus(Pair(childKey, newViewModel))
                )
            else
                currentDataState.copy(
                    weakViewModels = currentDataState.weakViewModels.plus(WeakReference(newViewModel))
                )

        mutableDataState.value = newDataState

        return newViewModel
    }

    protected abstract fun createChildViewModel(childKey: ChildKey): ChildModel

    private fun findChildViewModel(childKey: ChildKey, currentDataState: DataState<ChildKey, ChildModel>): ChildModel? =
        currentDataState.allActiveViewModels.firstOrNull { it.key == childKey }
}


abstract class ChildCommandManager<ChildState, ChildKey, ChildModel : ChildViewModel<ChildKey>>(
    private val commandManager: CommandManager<DataState<ChildKey, ChildModel>>,
    private val feedTypedLiveData: LiveData<ChildState>
) : CommandManager<ChildState> {

    override fun postCommand(actionCommand: ActionCommand<*, ChildState>) {
        commandManager.postCommand(
            DataConvertCommandSameResult(
                actionCommand,
                { outerData: DataState<ChildKey, ChildModel>, newInnerData: ChildState ->
                    applyNewDataToAllStates(outerData, newInnerData)
                },
                { feedTypedLiveData.value!! }
            )
        )
    }

    protected open fun applyNewDataToAllStates(
        usersRootState: DataState<ChildKey, ChildModel>,
        newUsersTypedState: ChildState
    ): DataState<ChildKey, ChildModel> {
        usersRootState.allActiveViewModels.forEach { updateViewModel(it, newUsersTypedState) }
        return usersRootState
    }

    protected abstract fun updateViewModel(childViewModel: ChildModel, newState: ChildState)

    override fun clearPendingCommands(clearRule: (ActionCommand<*, ChildState>) -> Boolean) {
        throw IllegalStateException("not supported here")
    }

    override fun blockExecutions() {
        throw IllegalStateException("not supported here")
    }

    override fun allowExecutions() {
        throw IllegalStateException("not supported here")
    }
}