package io.scal.commandbasedarchitecture.broadcast

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.scal.commandbasedarchitecture.ActionCommand
import io.scal.commandbasedarchitecture.CommandManager
import io.scal.commandbasedarchitecture.CommandManagerImpl
import io.scal.commandbasedarchitecture.DataConvertCommandSameResult
import java.lang.ref.WeakReference

interface ChildViewModel<ChildKey> {
    val key: ChildKey
}

/**
 * Broadcast data state class
 *
 * @param hardViewModels view models that should be kept permanently after the first request
 * @param weakViewModels view models that will be kept until cleared by gc
 */
data class DataState<ChildKey, ChildModel : ChildViewModel<ChildKey>>(
    val hardViewModels: Map<ChildKey, ChildModel>,
    val weakViewModels: List<WeakReference<ChildModel>>
) {

    val allActiveViewModels: List<ChildModel>
        get() = hardViewModels.values.plus(weakViewModels.mapNotNull { it.get() })
}

/**
 * ViewModel that will store all child view models and return already existing one with the same key.
 */
abstract class BaseBroadcastCommandViewModel<ChildKey, ChildModel : ChildViewModel<ChildKey>>(
    private val mainThreadScheduler: Scheduler
){

    private val mutableDataState =
        MutableLiveData<DataState<ChildKey, ChildModel>>(DataState(emptyMap(), emptyList()))

    protected val commandManager: CommandManager<DataState<ChildKey, ChildModel>> by lazy {
        createCommandManager(
            mutableDataState
        )
    }
    protected val compositeDisposable = CompositeDisposable()

    protected open fun createCommandManager(mutableDataState: MutableLiveData<DataState<ChildKey, ChildModel>>): CommandManager<DataState<ChildKey, ChildModel>> =
        CommandManagerImpl(mutableDataState, mainThreadScheduler, compositeDisposable)

    /**
     * Will get existing view model with the same key or create a new one
     *
     * @param hardViewModel is used to determinate how to store the newly constructed view model: hardViewModels
     * or weakViewModels
     * @see DataState
     */
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
                    hardViewModels = currentDataState.hardViewModels.plus(
                        Pair(
                            childKey,
                            newViewModel
                        )
                    )
                )
            else
                currentDataState.copy(
                    weakViewModels = currentDataState.weakViewModels.plus(WeakReference(newViewModel))
                )

        mutableDataState.value = newDataState

        return newViewModel
    }

    /**
     * Should construct new ChildView model for provided key
     */
    protected abstract fun createChildViewModel(childKey: ChildKey): ChildModel

    private fun findChildViewModel(
        childKey: ChildKey,
        currentDataState: DataState<ChildKey, ChildModel>
    ): ChildModel? =
        currentDataState.allActiveViewModels.firstOrNull { it.key == childKey }
}

/**
 * CommandManager that will broadcast all new state for all existing view models.
 * It is up to you how you will update each view model and their state.
 */
abstract class ChildCommandManager<ChildState, ChildKey, ChildModel : ChildViewModel<ChildKey>>(
    protected val key: ChildKey,
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

    /**
     * Called each time child view model state is updated to publish its state for all view models
     */
    protected open fun applyNewDataToAllStates(
        usersRootState: DataState<ChildKey, ChildModel>,
        newUsersTypedState: ChildState
    ): DataState<ChildKey, ChildModel> {
        usersRootState.allActiveViewModels.forEach { updateViewModel(it, newUsersTypedState) }
        return usersRootState
    }

    /**
     * Method for updating view model based on the new state. Not that this state may not be from this view model.
     */
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