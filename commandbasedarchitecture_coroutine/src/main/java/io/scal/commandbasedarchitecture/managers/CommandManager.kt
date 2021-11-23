package io.scal.commandbasedarchitecture.managers

import android.os.Looper
import androidx.annotation.MainThread
import io.scal.commandbasedarchitecture.commands.Command
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manager that executes commands with appropriate strategy while CoroutineScope is active
 */
abstract class CommandManager<State>(
    private val executionController: ExecutionController<State>,
    private val infoLoggerCallback: ((message: String) -> Unit)? = null,
    private val errorLoggerCallback: ((message: String, error: Throwable) -> Unit)? = null
) : ICommandManager<State>, IdleListener {

    private val activated = AtomicBoolean(true)

    init {
        executionController.addIdleStateListener(this)
    }

    override fun onIdle() {
        runPendingActions()
    }

    @MainThread
    override fun postCommand(actionCommand: Command<out Any?, State>) {
        addToPendingActionIfShould(actionCommand)
    }

    @MainThread
    override fun clearPendingCommands() {
        val wasCommands = executionController.getPendingCommands().size
        executionController.clearPendingCommands()
        logInfoMessage("Clear: was - $wasCommands, now - ${executionController.getPendingCommands().size}")
    }

    override fun blockExecutions() {
        activated.set(false)
        logInfoMessage("Execution: BLOCKED")
    }

    override fun allowExecutions() {
        activated.set(true)
        logInfoMessage("Execution: ALLOWED")

        runPendingActions()
    }

    private fun addToPendingActionIfShould(actionCommand: Command<out Any?, State>) {
        if (executionController.addToPendingCommandsIfShould(actionCommand)) {
            logInfoMessage("Adding: ADDED to the queue - $actionCommand")

            runPendingActions()
        } else {
            logInfoMessage("Adding: SKIPPED from the queue - $actionCommand")
        }
    }

    private fun runPendingActions() {
        if (!activated.get()) return

        executionController.getPendingCommands().forEach {
            logInfoMessage("Run: onCommandWasAdded for $it")

            setValueIfNotTheSame(
                it.onCommandWasAdded(getCurrentDataState())
            )
        }

        val firstCommand = executionController.getPendingCommands().firstOrNull() ?: return
        if (executionController.shouldBlockOtherCommand(firstCommand)) {
            logInfoMessage("Run: BLOCKED for $firstCommand")
            return
        }

        executionController.executeCommandIfAllowed(
            firstCommand,
            executionBody = {
                logInfoMessage("Run: ALLOWED for: $firstCommand")

                executeCommand(firstCommand)
            },
            commandFinished = { runPendingActions() }
        )

        val newFirstCommand = executionController.getPendingCommands().firstOrNull() ?: return
        if (firstCommand != newFirstCommand) {
            runPendingActions()
        }
    }

    private suspend fun <Result> executeCommand(actionCommand: Command<Result, State>) {
        logInfoMessage("Execute: STARTING - $actionCommand")
        try {
            setValueIfNotTheSame(
                actionCommand.onExecuteStarting(getCurrentDataState())
            )

            val result = actionCommand
                .executeCommandWithSideEffects(
                    {
                        checkMainThread()
                        getCurrentDataState()
                    },
                    {
                        checkMainThread()
                        setValueIfNotTheSame(it)
                    }
                )
            setValueIfNotTheSame(
                actionCommand.onExecuteSuccess(getCurrentDataState(), result)
            )

            logInfoMessage("Execute: EXECUTED - $actionCommand")
        } catch (e: Throwable) {
            setValueIfNotTheSame(
                actionCommand.onExecuteFail(getCurrentDataState(), e)
            )

            logErrorMessage("Execute: FAILED - $actionCommand, $e", e)
        }
        setValueIfNotTheSame(
            actionCommand.onExecuteFinished(getCurrentDataState())
        )
    }

    protected abstract fun getCurrentDataState(): State

    protected abstract fun setValueIfNotTheSame(newState: State)

    private fun logInfoMessage(message: String) {
        infoLoggerCallback?.invoke(message)
    }

    private fun logErrorMessage(message: String, e: Throwable) {
        errorLoggerCallback?.invoke(message, e)
    }

    private fun checkMainThread() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw IllegalStateException("can be called only on MainThread")
        }
    }
}