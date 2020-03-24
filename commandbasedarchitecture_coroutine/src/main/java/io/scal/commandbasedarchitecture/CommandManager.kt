package io.scal.commandbasedarchitecture

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manager that is able to execute commands with appropriate strategy while CoroutineScope is active
 */
interface CommandManager<State> {

    @MainThread
    fun postCommand(actionCommand: ActionCommand<*, State>)

    @MainThread
    fun clearPendingCommands(clearRule: (ActionCommand<*, State>) -> Boolean)

    @MainThread
    fun blockExecutions()

    @MainThread
    fun allowExecutions()
}

@Suppress("unused")
class CommandManagerImpl<State>(
    private val dataState: MutableLiveData<State>,
    private val coroutineScope: CoroutineScope
) : CommandManager<State> {

    private val activated = AtomicBoolean(true)

    private val pendingActionCommands = mutableListOf<ActionCommand<*, State>>()
    private val runningActionCommands = mutableListOf<ActionCommand<*, State>>()

    /**
     * Will post command to execution queue.
     * Command may not be added to the pending queue - strategy rules this.
     */
    @MainThread
    override fun postCommand(actionCommand: ActionCommand<*, State>) {
        addToPendingActionsIfShould(actionCommand)
    }

    @MainThread
    override fun clearPendingCommands(clearRule: (ActionCommand<*, State>) -> Boolean) {
        pendingActionCommands.removeAll(clearRule)
    }

    @MainThread
    override fun blockExecutions() {
        activated.set(false)
    }

    @MainThread
    override fun allowExecutions() {
        activated.set(true)

        runPendingActions()
    }

    private fun addToPendingActionsIfShould(actionCommand: ActionCommand<*, State>) {
        if (actionCommand
                .shouldAddToPendingActions(
                    getCurrentDataState(),
                    pendingActionCommands.toRemoveOnlyList(),
                    runningActionCommands.toList()
                )
        ) {
            pendingActionCommands.add(actionCommand)

            runPendingActions()
        }
    }

    private fun runPendingActions() {
        if (!activated.get()) return

        pendingActionCommands.forEach {
            dataState.setValueIfNotTheSame(
                it.onCommandWasAdded(getCurrentDataState())
            )
        }

        val firstCommand = pendingActionCommands.firstOrNull() ?: return
        if (runningActionCommands.any { it.shouldBlockOtherTask(firstCommand) }) {
            return
        }
        if (firstCommand.shouldExecuteAction(
                getCurrentDataState(),
                pendingActionCommands.toRemoveOnlyList(),
                runningActionCommands.toList()
            )
        ) {
            pendingActionCommands.remove(firstCommand)
            runningActionCommands.add(firstCommand)
            executeCommand(firstCommand)
        }

        val newFirstCommand = pendingActionCommands.firstOrNull() ?: return
        if (firstCommand != newFirstCommand) {
            runPendingActions()
        }
    }

    private fun <Result> executeCommand(actionCommand: ActionCommand<Result, State>) {
        coroutineScope
            .launch(Dispatchers.Main) {
                try {
                    dataState.setValueIfNotTheSame(
                        actionCommand.onExecuteStarting(getCurrentDataState())
                    )

                    val result = actionCommand.executeCommand(getCurrentDataState())
                    dataState.setValueIfNotTheSame(
                        actionCommand.onExecuteSuccess(getCurrentDataState(), result)
                    )
                } catch (e: Throwable) {
                    dataState.setValueIfNotTheSame(
                        actionCommand.onExecuteFail(getCurrentDataState(), e)
                    )
                }
                dataState.setValueIfNotTheSame(
                    actionCommand.onExecuteFinished(getCurrentDataState())
                )
            }
            .invokeOnCompletion {
                runningActionCommands.remove(actionCommand)

                if (null == it) runPendingActions()
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCurrentDataState(): State =
        dataState.value as State
}

private fun <T> MutableLiveData<T>.setValueIfNotTheSame(newState: T) {
    if (value != newState) {
        value = newState
    }
}
