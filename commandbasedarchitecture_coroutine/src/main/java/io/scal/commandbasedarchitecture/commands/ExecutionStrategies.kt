package io.scal.commandbasedarchitecture.commands

import io.scal.commandbasedarchitecture.model.RemoveOnlyList
import io.scal.commandbasedarchitecture.model.removeAll

/**
 * Strategy that will be used by CommandManager to control command execution flow.
 */
interface ExecutionStrategy {

    /**
     * Called before adding command to pending.
     * If returns false - command will be skipped and never be executed.
     * If true - command will be executed somewhere in the future.
     * @return if the command should be added to execution queue.
     */
    fun shouldAddToPendingActions(
        pendingActionCommands: RemoveOnlyList<Command<*, *>>,
        runningActionCommands: List<Command<*, *>>
    ): Boolean

    /**
     * Called before pendingActionCommand execution to know if this command should wait some time.
     * Calls during the execution phase of the command with implementing strategy.
     * @return true if command should wait
     */
    fun shouldBlockOtherTask(pendingActionCommand: Command<*, *>): Boolean

    /**
     * Called on the command that want to execute to know if it is a good time for this.
     * @return true if command should be executed
     */
    fun shouldExecuteAction(
        pendingActionCommands: RemoveOnlyList<Command<*, *>>,
        runningActionCommands: List<Command<*, *>>
    ): Boolean
}

/**
 * Allows concurrent execution always.
 */
open class ConcurrentStrategy : ExecutionStrategy {

    override fun shouldAddToPendingActions(
        pendingActionCommands: RemoveOnlyList<Command<*, *>>,
        runningActionCommands: List<Command<*, *>>
    ): Boolean =
        true

    override fun shouldBlockOtherTask(pendingActionCommand: Command<*, *>): Boolean =
        false

    override fun shouldExecuteAction(
        pendingActionCommands: RemoveOnlyList<Command<*, *>>,
        runningActionCommands: List<Command<*, *>>
    ): Boolean =
        true
}

/**
 * Allows concurrent execution only if there are no running commands with same strategy and tag.
 * Also will remove any pending command with the same tag and replace with a new one.
 */
open class ConcurrentStrategyWithTag(private val tag: Any) : ConcurrentStrategy() {

    override fun shouldAddToPendingActions(
        pendingActionCommands: RemoveOnlyList<Command<*, *>>,
        runningActionCommands: List<Command<*, *>>
    ): Boolean {
        pendingActionCommands.removeAll { command ->
            command.strategy.let { it is ConcurrentStrategyWithTag && it.tag == tag }
        }
        return true
    }

    override fun shouldExecuteAction(
        pendingActionCommands: RemoveOnlyList<Command<*, *>>,
        runningActionCommands: List<Command<*, *>>
    ): Boolean =
        !runningActionCommands.any { command ->
            command.strategy.let { it is ConcurrentStrategyWithTag && it.tag == tag }
        }
}

/**
 * Will be added to the queue only if there are no other tasks with this strategy in pending and running queues.
 * Will be executed only as a single task and block all other tasks from execution.
 */
open class SingleStrategy : ExecutionStrategy {

    override fun shouldAddToPendingActions(
        pendingActionCommands: RemoveOnlyList<Command<*, *>>,
        runningActionCommands: List<Command<*, *>>
    ): Boolean =
        !pendingActionCommands.any { it.strategy is SingleStrategy }
                && !runningActionCommands.any { it.strategy is SingleStrategy }

    override fun shouldBlockOtherTask(pendingActionCommand: Command<*, *>): Boolean =
        true

    override fun shouldExecuteAction(
        pendingActionCommands: RemoveOnlyList<Command<*, *>>,
        runningActionCommands: List<Command<*, *>>
    ): Boolean =
        runningActionCommands.isEmpty()
}

/**
 * Same as SingleStrategy but will be added to the pending queue only if there are no pending or
 * running task with a strategy of the same tag.
 */
open class SingleWithTagStrategy(private val tag: Any) : SingleStrategy() {

    override fun shouldAddToPendingActions(
        pendingActionCommands: RemoveOnlyList<Command<*, *>>,
        runningActionCommands: List<Command<*, *>>
    ): Boolean =
        !pendingActionCommands.any { command ->
            command.strategy.let { it is SingleWithTagStrategy && it.tag == tag }
        }
                &&
                !runningActionCommands.any { command ->
                    command.strategy.let { it is SingleWithTagStrategy && it.tag == tag }
                }
}