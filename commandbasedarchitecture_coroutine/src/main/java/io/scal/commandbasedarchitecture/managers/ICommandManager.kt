package io.scal.commandbasedarchitecture.managers

import androidx.annotation.MainThread
import io.scal.commandbasedarchitecture.commands.Command

/**
 * Manager that is able to execute commands
 */
interface ICommandManager<State> {

    /**
     * Add command to pending queue if command allows that
     * @see io.scal.commandbasedarchitecture.commands.ExecutionStrategy
     */
    @MainThread
    fun postCommand(actionCommand: Command<out Any?, State>)

    /**
     * Remove all pending commands that fit the rule
     */
    @MainThread
    fun clearPendingCommands()

    /**
     * Will block any pending commands from run. All currently running tasks will be executed normally
     */
    fun blockExecutions()

    /**
     * Allow normal execution flow
     */
    fun allowExecutions()
}