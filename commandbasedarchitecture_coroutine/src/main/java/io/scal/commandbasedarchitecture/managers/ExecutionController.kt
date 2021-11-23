package io.scal.commandbasedarchitecture.managers

import io.scal.commandbasedarchitecture.commands.Command
import kotlinx.coroutines.*

open class ExecutionController<State : Any?>(
    private val coroutineScope: CoroutineScope,
    private val executionController: ExecutionController<Any?>? = null
) {

    protected val pendingCommands = mutableListOf<Command<*, State>>()
    protected val runningCommands = mutableListOf<Command<*, *>>()

    open fun clearPendingCommands() {
        pendingCommands.clear()
    }

    open fun addToPendingCommandsIfShould(command: Command<*, State>): Boolean =
        if (
            command.shouldAddToPendingCommands(pendingCommands.toList(), runningCommands.toList())
        ) {
            pendingCommands.add(command)

            true
        } else {
            false
        }

    open fun executeCommandIfAllowed(
        command: Command<*, *>,
        executionBody: suspend () -> Unit,
        commandFinished: () -> Unit
    ) {
        if (
            coroutineScope.isActive &&
            command.shouldExecute(pendingCommands.toList(), runningCommands.toList())
        ) {
            if (null == executionController) {
                pendingCommands.remove(command)
                runningCommands.add(command)

                coroutineScope
                    .launch(Dispatchers.Main) { executionBody() }
                    .invokeOnCompletion {
                        runningCommands.remove(command)

                        commandFinished()
                    }
            } else {
                executionController.executeCommandIfAllowed(
                    command,
                    {
                        pendingCommands.remove(command)
                        runningCommands.add(command)

                        withContext(coroutineScope.coroutineContext + Dispatchers.Main) {
                            try {
                                executionBody()
                            } finally {
                                runningCommands.remove(command)

                                commandFinished()
                            }
                        }
                    },
                    { commandFinished() }
                )
            }
        }
    }

    internal fun getPendingCommands(): List<Command<*, State>> =
        pendingCommands.toList()

    internal fun shouldBlockOtherCommand(command: Command<*, *>): Boolean =
        runningCommands.any { it.shouldBlockOtherCommand(command) }
                || (executionController?.shouldBlockOtherCommand(command) ?: false)
}