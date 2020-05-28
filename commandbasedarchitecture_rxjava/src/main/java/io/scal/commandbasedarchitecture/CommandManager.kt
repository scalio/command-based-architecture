package io.scal.commandbasedarchitecture

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.scal.commandbasedarchitecture.model.toRemoveOnlyList
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
    private val mainThreadScheduler: Scheduler,
    private val compositeDisposable: CompositeDisposable,
    private val infoLoggerCallback: ((message: String) -> Unit)? = null,
    private val errorLoggerCallback: ((message: String, error: Throwable) -> Unit)? = null
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
        logInfoMessage("Clear: was - $wasCommands, now - ${pendingActionCommands.size}")
    }

    @MainThread
    override fun blockExecutions() {
        activated.set(false)
        logInfoMessage("Execution: BLOCKED")
    }

    @MainThread
    override fun allowExecutions() {
        activated.set(true)
        logInfoMessage("Execution: ALLOWED")

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
            logInfoMessage("Adding: ADDED to the queue - $actionCommand")

            pendingActionCommands.add(actionCommand)

            runPendingActions()
        } else {
            logInfoMessage("Adding: SKIPPED from the queue - $actionCommand")
        }
    }

    private fun runPendingActions() {
        if (!activated.get()) return

        pendingActionCommands.forEach {
            logInfoMessage("Run: onCommandWasAdded for $it")

            dataState.setValueIfNotTheSame(
                it.onCommandWasAdded(getCurrentDataState())
            )
        }

        val firstCommand = pendingActionCommands.firstOrNull() ?: return
        if (runningActionCommands.any { it.shouldBlockOtherTask(firstCommand) }) {
            logInfoMessage("Run: BLOCKED for $firstCommand")
            return
        }
        if (firstCommand.shouldExecuteAction(
                getCurrentDataState(),
                pendingActionCommands.toRemoveOnlyList(),
                runningActionCommands.toList()
            )
        ) {
            logInfoMessage("Run: ALLOWED for: $firstCommand")

            pendingActionCommands.remove(firstCommand)
            runningActionCommands.add(firstCommand)
            executeCommand(firstCommand)
        } else {
            logInfoMessage("Run: POSTPONED for: $firstCommand")
        }

        val newFirstCommand = pendingActionCommands.firstOrNull() ?: return
        if (firstCommand != newFirstCommand) {
            runPendingActions()
        }
    }

    private fun <Result> executeCommand(actionCommand: ActionCommand<Result, State>) {
        val disposable = Completable
            .fromAction {
                logInfoMessage("Execute: STARTING - $actionCommand")

                dataState.setValueIfNotTheSame(
                    actionCommand.onExecuteStarting(getCurrentDataState())
                )
            }
            .subscribeOn(mainThreadScheduler)
            .observeOn(mainThreadScheduler)
            .andThen(
                Single
                    .fromCallable { getCurrentDataState() }
                    .flatMap { actionCommand.executeCommand(it) }
            )
            .observeOn(mainThreadScheduler)
            .doOnDispose {
                logInfoMessage("Execute: CANCELLED - $actionCommand")

                runningActionCommands.remove(actionCommand)
            }
            .subscribe(
                {
                    logInfoMessage("Execute: EXECUTED - $actionCommand")

                    dataState.setValueIfNotTheSame(
                        actionCommand.onExecuteSuccess(getCurrentDataState(), it)
                    )

                    onRunningTaskFinished(actionCommand)
                },
                {
                    dataState.setValueIfNotTheSame(
                        actionCommand.onExecuteFail(getCurrentDataState(), it)
                    )

                    logErrorMessage("Execute: FAILED - $actionCommand, $it", it)

                    onRunningTaskFinished(actionCommand)
                }
            )
        compositeDisposable.add(disposable)
    }

    private fun <Result> onRunningTaskFinished(actionCommand: ActionCommand<Result, State>) {
        dataState.setValueIfNotTheSame(
            actionCommand.onExecuteFinished(getCurrentDataState())
        )
        runningActionCommands.remove(actionCommand)

        logInfoMessage("Execute: FINISHED - $actionCommand")

        runPendingActions()
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCurrentDataState(): State =
        dataState.value as State

    private fun logInfoMessage(message: String) {
        infoLoggerCallback?.invoke(message)
    }

    private fun logErrorMessage(message: String, e: Throwable) {
        errorLoggerCallback?.invoke(message, e)
    }
}

private fun <T> MutableLiveData<T>.setValueIfNotTheSame(newState: T) {
    if (value != newState) {
        value = newState
    }
}
