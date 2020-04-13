package io.scal.commandbasedarchitecture

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import io.scal.commandbasedarchitecture.model.toRemoveOnlyList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manager that is able to execute commands
 */
interface CommandManager<State> {

    /**
     * Add command to pending queue if command allows that
     * @see ExecutionStrategy
     */
    @MainThread
    fun postCommand(actionCommand: ActionCommand<*, State>)

    /**
     * Remove all pending commands that fit the rule
     */
    @MainThread
    fun clearPendingCommands(clearRule: (ActionCommand<*, State>) -> Boolean)

    /**
     * Will block any pending commands from run. All currently running tasks will be executed normally
     */
    @MainThread
    fun blockExecutions()

    /**
     * Allow normal execution flow
     */
    @MainThread
    fun allowExecutions()
}

/**
 * Manager that executes commands with appropriate strategy while CoroutineScope is active
 */
class CommandManagerImpl<State>(
    private val dataState: MutableLiveData<State>,
    private val coroutineScope: CoroutineScope,
    private val loggerCallback: ((message: String) -> Unit)? = null
) : CommandManager<State> {

    private val activated = AtomicBoolean(true)

    private val pendingActionCommands = mutableListOf<ActionCommand<*, State>>()
    private val runningActionCommands = mutableListOf<ActionCommand<*, State>>()

    @MainThread
    override fun postCommand(actionCommand: ActionCommand<*, State>) {
        addToPendingActionsIfShould(actionCommand)
    }

    @MainThread
    override fun clearPendingCommands(clearRule: (ActionCommand<*, State>) -> Boolean) {
        val wasCommands = pendingActionCommands.size
        pendingActionCommands.removeAll(clearRule)
        logMessage("Clear: was - $wasCommands, now - ${pendingActionCommands.size}")
    }

    @MainThread
    override fun blockExecutions() {
        activated.set(false)
        logMessage("Execution: BLOCKED")
    }

    @MainThread
    override fun allowExecutions() {
        activated.set(true)
        logMessage("Execution: ALLOWED")

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
            logMessage("Adding: ADDED to the queue - $actionCommand")

            pendingActionCommands.add(actionCommand)

            runPendingActions()
        } else {
            logMessage("Adding: SKIPPED from the queue - $actionCommand")
        }
    }

    private fun runPendingActions() {
        if (!activated.get()) return

        pendingActionCommands.forEach {
            logMessage("Run: onCommandWasAdded for $it")

            dataState.setValueIfNotTheSame(
                it.onCommandWasAdded(getCurrentDataState())
            )
        }

        val firstCommand = pendingActionCommands.firstOrNull() ?: return
        if (runningActionCommands.any { it.shouldBlockOtherTask(firstCommand) }) {
            logMessage("Run: BLOCKED for $firstCommand")
            return
        }
        if (firstCommand.shouldExecuteAction(
                getCurrentDataState(),
                pendingActionCommands.toRemoveOnlyList(),
                runningActionCommands.toList()
            )
        ) {
            logMessage("Run: ALLOWED for: $firstCommand")

            pendingActionCommands.remove(firstCommand)
            runningActionCommands.add(firstCommand)
            executeCommand(firstCommand)
        } else {
            logMessage("Run: POSTPONED for: $firstCommand")
        }

        val newFirstCommand = pendingActionCommands.firstOrNull() ?: return
        if (firstCommand != newFirstCommand) {
            runPendingActions()
        }
    }

    private fun <Result> executeCommand(actionCommand: ActionCommand<Result, State>) {
        coroutineScope
            .launch(Dispatchers.Main) {
                logMessage("Execute: STARTING - $actionCommand")
                try {
                    dataState.setValueIfNotTheSame(
                        actionCommand.onExecuteStarting(getCurrentDataState())
                    )

                    val result = actionCommand.executeCommand(getCurrentDataState())
                    dataState.setValueIfNotTheSame(
                        actionCommand.onExecuteSuccess(getCurrentDataState(), result)
                    )

                    logMessage("Execute: EXECUTED - $actionCommand")
                } catch (e: Throwable) {
                    dataState.setValueIfNotTheSame(
                        actionCommand.onExecuteFail(getCurrentDataState(), e)
                    )

                    logMessage("Execute: FAILED - $actionCommand, $e")
                }
                dataState.setValueIfNotTheSame(
                    actionCommand.onExecuteFinished(getCurrentDataState())
                )
            }
            .invokeOnCompletion {
                runningActionCommands.remove(actionCommand)

                if (null == it) {
                    logMessage("Execute: FINISHED - $actionCommand")
                    runPendingActions()
                } else {
                    logMessage("Execute: CANCELLED - $actionCommand")
                }
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCurrentDataState(): State =
        dataState.value as State

    private fun logMessage(message: String) {
        loggerCallback?.invoke(message)
    }
}

private fun <T> MutableLiveData<T>.setValueIfNotTheSame(newState: T) {
    if (value != newState) {
        value = newState
    }
}