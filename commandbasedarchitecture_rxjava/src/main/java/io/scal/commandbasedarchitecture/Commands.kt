package io.scal.commandbasedarchitecture

import io.reactivex.Single
import io.scal.commandbasedarchitecture.model.RemoveOnlyList
import io.scal.commandbasedarchitecture.model.removeAll

/**
 * Base class for all commands.
 * This command should implement all filtering by itself.
 */
abstract class ActionCommand<CommandResult, DataState : Any?> {

    open val strategy: ExecutionStrategy? = null

    /**
     * Called when command was added to pending commands and is awaiting execution.
     * Good place to change data based for immediate ui update.
     *
     * This method may be called multiple times
     */
    open fun onCommandWasAdded(dataState: DataState): DataState = dataState

    /**
     * Called right before command starts execution
     *
     * This method may be called only once
     */
    open fun onExecuteStarting(dataState: DataState): DataState = dataState

    /**
     * Main command execution method. Method may execute normally or with exception.
     * Normal execution will trigger onExecuteSuccess, fail - onExecuteFail
     *
     * This method may be called only once
     *
     * @see ActionCommand.onExecuteSuccess
     * @see ActionCommand.onExecuteFail
     */
    abstract fun executeCommand(dataState: DataState): Single<CommandResult>

    /**
     * Called if command executed normally
     *
     * This method may be called only once
     *
     * @param result result of the execution
     */
    open fun onExecuteSuccess(dataState: DataState, result: CommandResult): DataState = dataState

    /**
     * Called if command executed normally
     *
     * This method may be called only once
     *
     * @param error exception that was thrown during execution
     */
    open fun onExecuteFail(dataState: DataState, error: Throwable): DataState = dataState

    /**
     * Always called after success or fail
     *
     * This method may be called only once
     */
    open fun onExecuteFinished(dataState: DataState): DataState = dataState

    /**
     * Controls if command should be added to pending queue or not based on current data state
     *
     * This method may be called multiple times
     *
     * @return true if should be added or false if should be dropped
     */
    abstract fun shouldAddToPendingActions(
        dataState: DataState,
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean

    /**
     * Method to control other tasks.
     * Will be called only if current task is in execution state and pendingActionCommand needs to be executed immediately.
     *
     * This method may be called multiple times
     *
     * @return true if pendingActionCommand should wait some time (usually for current command execution finish), false if other command can be executed in parallel mode
     */
    abstract fun shouldBlockOtherTask(pendingActionCommand: ActionCommand<*, *>): Boolean

    /**
     * Method to control that current command is able to execute immediately.
     *
     * This method may be called multiple times
     *
     * @return true if current command is able execute immediately, false - if command should wait some time
     */
    abstract fun shouldExecuteAction(
        dataState: DataState,
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean
}


/**
 * Command that routes all execution strategy methods to a separate class StateStrategy
 * @see ExecutionStrategy
 */
abstract class ActionCommandWithStrategy<CommandResult, DataState : Any?>(
    override val strategy: ExecutionStrategy
) : ActionCommand<CommandResult, DataState>() {

    override fun shouldAddToPendingActions(
        dataState: DataState,
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        strategy.shouldAddToPendingActions(pendingActionCommands, runningActionCommands)

    override fun shouldBlockOtherTask(pendingActionCommand: ActionCommand<*, *>): Boolean =
        strategy.shouldBlockOtherTask(pendingActionCommand)

    override fun shouldExecuteAction(
        dataState: DataState,
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        strategy.shouldExecuteAction(pendingActionCommands, runningActionCommands)
}

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
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean

    /**
     * Called before pendingActionCommand execution to know if this command should wait some time.
     * Calls during the execution phase of the command with implementing strategy.
     * @return true if command should wait
     */
    fun shouldBlockOtherTask(pendingActionCommand: ActionCommand<*, *>): Boolean

    /**
     * Called on the command that want to execute to know if it is a good time for this.
     * @return true if command should be executed
     */
    fun shouldExecuteAction(
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean
}

/**
 * Allows concurrent execution always.
 */
open class ConcurrentStrategy : ExecutionStrategy {

    override fun shouldAddToPendingActions(
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        true

    override fun shouldBlockOtherTask(pendingActionCommand: ActionCommand<*, *>): Boolean =
        false

    override fun shouldExecuteAction(
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        true
}

/**
 * Allows concurrent execution only if there are no running commands with same strategy and tag.
 * Also will remove any pending command with the same tag and replace with a new one.
 */
open class ConcurrentStrategyWithTag(private val tag: Any) : ConcurrentStrategy() {

    override fun shouldAddToPendingActions(
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean {
        pendingActionCommands.removeAll { command ->
            command.strategy.let { it is ConcurrentStrategyWithTag && it.tag == tag }
        }
        return true
    }

    override fun shouldExecuteAction(
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
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
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        !pendingActionCommands.any { it.strategy is SingleStrategy }
                && !runningActionCommands.any { it.strategy is SingleStrategy }

    override fun shouldBlockOtherTask(pendingActionCommand: ActionCommand<*, *>): Boolean =
        true

    override fun shouldExecuteAction(
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        runningActionCommands.isEmpty()
}

/**
 * Same as SingleStrategy but will be added to the pending queue only if there are no pending or
 * running task with a strategy of the same tag.
 */
open class SingleWithTagStrategy(private val tag: Any) : SingleStrategy() {

    override fun shouldAddToPendingActions(
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        !pendingActionCommands.any { command ->
            command.strategy.let { it is SingleWithTagStrategy && it.tag == tag }
        }
                &&
                !runningActionCommands.any { command ->
                    command.strategy.let { it is SingleWithTagStrategy && it.tag == tag }
                }
}