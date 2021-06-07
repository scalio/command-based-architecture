package io.scal.commandbasedarchitecture.managers

import io.scal.commandbasedarchitecture.commands.Command
import io.scal.commandbasedarchitecture.model.toRemoveOnlyList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

open class ExecutionController<State>(
    private val coroutineScope: CoroutineScope,
) {

    protected val pendingCommands = mutableListOf<Command<*, State>>()
    protected val runningCommands = mutableListOf<Command<*, State>>()

    open fun clearPendingCommands(clearRule: (Command<*, State>) -> Boolean) {
        pendingCommands.removeAll(clearRule)
    }

    open fun addToPendingCommandsIfShould(command: Command<*, State>): Boolean =
        if (command.shouldAddToPendingCommands(
                pendingCommands.toRemoveOnlyList(),
                runningCommands.toList()
            )
        ) {
            pendingCommands.add(command)

            true
        } else {
            false
        }

    open fun executeCommandIfAllowed(
        command: Command<*, State>,
        executionBody: suspend () -> Unit,
        commandFinished: () -> Unit
    ) {
        if (
            coroutineScope.isActive &&
            command.shouldExecute(pendingCommands.toRemoveOnlyList(), runningCommands.toList())
        ) {
            pendingCommands.remove(command)
            runningCommands.add(command)

            coroutineScope
                .launch(Dispatchers.Main) { executionBody() }
                .invokeOnCompletion {
                    runningCommands.remove(command)

                    commandFinished()
                }
        }
    }

    internal fun getPendingCommands(): List<Command<*, State>> =
        pendingCommands

    internal fun getRunningCommands(): List<Command<*, State>> =
        runningCommands
}