package io.scal.commandbasedarchitecture.broadcast

import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.scal.commandbasedarchitecture.ActionCommand
import io.scal.commandbasedarchitecture.CommandManager
import io.scal.commandbasedarchitecture.CommandManagerImpl
import io.scal.commandbasedarchitecture.DataConvertCommandSameResult
import io.scal.commandbasedarchitecture.model.readNullOrValue
import io.scal.commandbasedarchitecture.model.writeToParcel
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import java.lang.ref.SoftReference

interface ChildViewModel<ChildState> {
    val fullDataState: LiveData<ChildState>
}

/**
 * Broadcast data state class
 *
 * @param strongViewStates view models that should be kept permanently after the first request
 * @param weakViewStates view models that will be kept until cleared by gc
 */
@Parcelize
data class DataState<ChildKey, ChildState>(
    val strongViewStates: Map<ChildKey, MutableLiveData<ChildState>>,
    val weakViewStates: Map<ChildKey, SoftReference<MutableLiveData<ChildState>>>
) : Parcelable {

    val allActiveChildStatesAsMap: Map<ChildKey, MutableLiveData<ChildState>>
        get() = strongViewStates
            .plus(weakViewStates
                .mapValues { it.value.get() }
                .filterValues { null != it }
                .mapValues { it.value!! }
            )

    val allActiveChildStatesAsList: List<MutableLiveData<ChildState>>
        get() = strongViewStates.values.plus(weakViewStates.mapNotNull { it.value.get() })

    companion object : Parceler<DataState<Any?, Any?>> {

        override fun DataState<Any?, Any?>.write(parcel: Parcel, flags: Int) {
            val allStates = allActiveChildStatesAsMap

            parcel.writeInt(allStates.size)
            allStates.forEach { (childKey, liveData) ->
                childKey.writeToParcel(parcel)
                liveData.value.writeToParcel(parcel)
            }
        }

        override fun create(parcel: Parcel): DataState<Any?, Any?> {
            val hardModelsMap = mutableMapOf<Any?, MutableLiveData<Any?>>()
            val count = parcel.readInt()
            repeat(count) {
                val key = parcel.readNullOrValue()
                val value = parcel.readNullOrValue()
                if (null != value) {
                    hardModelsMap[key] = MutableLiveData(value)
                }
            }

            return DataState(hardModelsMap, emptyMap())
        }
    }
}

/**
 * ViewModel that will store all child view models and return already existing one with the same key.
 */
abstract class BaseBroadcastCommandViewModel<ChildKey, ChildState, ChildModel : ChildViewModel<ChildState>>(
    private val mainThreadScheduler: Scheduler,
    private val loggerCallback: ((message: String) -> Unit)? = null,
    private val errorLoggerCallback: ((message: String, error: Throwable) -> Unit)? = null
) {

    protected open val mutableDataState =
        MutableLiveData<DataState<ChildKey, ChildState>>(DataState(emptyMap(), emptyMap()))

    protected open val commandManager: CommandManager<DataState<ChildKey, ChildState>> by lazy {
        createCommandManager(
            mutableDataState
        )
    }
    protected open val compositeDisposable = CompositeDisposable()

    protected open fun createCommandManager(mutableDataState: MutableLiveData<DataState<ChildKey, ChildState>>): CommandManager<DataState<ChildKey, ChildState>> =
        CommandManagerImpl(
            mutableDataState,
            mainThreadScheduler,
            compositeDisposable,
            loggerCallback,
            errorLoggerCallback
        )

    /**
     * Will get existing view model with the same key or create a new one
     *
     * @param hardViewModel is used to determinate how to store the newly constructed view model: hardViewModels
     * or weakViewModels
     * @see DataState
     */
    protected fun getChildViewModel(childKey: ChildKey, hardViewModel: Boolean): ChildModel {
        val currentDataState = mutableDataState.value!!
        val cachedChildState = findChildState(childKey, currentDataState)

        val newViewModel = createChildViewModel(childKey, cachedChildState)

        val newDataState =
            if (hardViewModel)
                currentDataState.copy(
                    strongViewStates = currentDataState.strongViewStates
                        .plus(
                            Pair(
                                childKey,
                                newViewModel.fullDataState as MutableLiveData<ChildState>
                            )
                        )
                )
            else
                currentDataState.copy(
                    weakViewStates = currentDataState.weakViewStates
                        .plus(
                            Pair(
                                childKey,
                                SoftReference(newViewModel.fullDataState as MutableLiveData<ChildState>)
                            )
                        )
                )
        mutableDataState.value = newDataState

        return newViewModel
    }

    /**
     * Should construct new ChildView model for provided key
     */
    protected abstract fun createChildViewModel(
        childKey: ChildKey,
        cachedChildState: MutableLiveData<ChildState>?
    ): ChildModel

    private fun findChildState(
        childKey: ChildKey,
        currentDataState: DataState<ChildKey, ChildState>
    ): MutableLiveData<ChildState>? =
        currentDataState.allActiveChildStatesAsMap[childKey]
}

/**
 * CommandManager that will broadcast all new state for all existing view models.
 * It is up to you how you will update each view model and their state.
 */
abstract class ChildCommandManager<ChildKey, ChildState>(
    protected val key: ChildKey,
    private val commandManager: CommandManager<DataState<ChildKey, ChildState>>,
    private val feedTypedLiveData: LiveData<ChildState>
) : CommandManager<ChildState> {

    override fun postCommand(actionCommand: ActionCommand<*, ChildState>) {
        commandManager.postCommand(
            DataConvertCommandSameResult(
                actionCommand,
                { outerData: DataState<ChildKey, ChildState>, newInnerData: ChildState ->
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
        usersRootState: DataState<ChildKey, ChildState>,
        newUsersTypedState: ChildState
    ): DataState<ChildKey, ChildState> {
        usersRootState.allActiveChildStatesAsMap.forEach {
            updateState(it.key, it.value, newUsersTypedState)
        }
        return usersRootState
    }

    /**
     * Method for updating view model based on the new state. Not that this state may not be from this view model.
     */
    protected abstract fun updateState(
        stateToUpdateKey: ChildKey,
        stateToUpdateLiveData: MutableLiveData<ChildState>,
        newState: ChildState
    )

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