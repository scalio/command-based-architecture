package io.scal.commandbasedarchitecture

/**
 * Base class for all commands.
 * This command should implement all filtering by itself.
 */
abstract class ActionCommand<CommandResult, DataState : Any?> {

    open val strategy: StateStrategy? = null

    /**
     * Called when command was allowed to pending commands and is waiting for execution.
     * Good place to change data based for immediate ui update.
     */
    open fun onCommandWasAdded(dataState: DataState): DataState = dataState

    /**
     * Called right before command start execution.
     */
    open fun onExecuteStarting(dataState: DataState): DataState = dataState

    /**
     * Main command execution method. Method may execute normally or with exception.
     * Normal execution will trigger onExecuteSuccess, fail - onExecuteFail
     *
     * @see ActionCommand.onExecuteSuccess
     * @see ActionCommand.onExecuteFail
     */
    abstract suspend fun executeCommand(dataState: DataState): CommandResult

    /**
     * Called if command executed normally.
     * @param result result of the execution
     */
    open fun onExecuteSuccess(dataState: DataState, result: CommandResult): DataState = dataState

    /**
     * Called if command executed normally
     * @param error exception that was thrown during execution
     */
    open fun onExecuteFail(dataState: DataState, error: Throwable): DataState = dataState

    /**
     * Always called after success or fail
     */
    open fun onExecuteFinished(dataState: DataState): DataState = dataState

    /**
     * Controls if command should be added to pending quire or not based on current data state
     * @return true if should be added or false if should be dropped
     */
    abstract fun shouldAddToPendingActions(
        dataState: DataState,
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean

    /**
     * Method to control other tasks.
     * Will be called only if current task is in execution state and pendingActionCommand wants to be executed right now.
     *
     * @return true if pendingActionCommand should wait some time (usually for current command execution finish), false if other command can be executed in parallel mode
     */
    abstract fun shouldBlockOtherTask(pendingActionCommand: ActionCommand<*, *>): Boolean

    /**
     * Method to control that current command is able to execute now.
     *
     * @return true if current command is able execute right now, false - if command should wait some time
     */
    abstract fun shouldExecuteAction(
        dataState: DataState,
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean
}


/**
 * Command that route all execution strategy methods to a separate class StateStrategy
 * @see StateStrategy
 */
abstract class ActionCommandWithStrategy<CommandResult, DataState : Any?>(
    override val strategy: StateStrategy
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
interface StateStrategy {

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
 * If tag is null - allows concurrent execution always.
 *
 * If tag is not null - allows concurrent execution only if there are no running commands with same strategy and tag.
 * Also will remove any pending command with the same tag and replace by a new one.
 */
open class ConcurrentStrategy(private val tag: String? = null) : StateStrategy {

    override fun shouldAddToPendingActions(
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        if (null == tag) {
            true
        } else {
            pendingActionCommands.removeAll { command ->
                command.strategy.let { it is ConcurrentStrategy && it.tag == tag }
            }
            true
        }

    override fun shouldBlockOtherTask(pendingActionCommand: ActionCommand<*, *>): Boolean =
        false

    override fun shouldExecuteAction(
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        if (null == tag) {
            true
        } else {
            !runningActionCommands.any { command ->
                command.strategy.let { it is ConcurrentStrategy && it.tag == tag }
            }
        }
}

/**
 * Will be added to the queue only if there are no other tasks with this strategy in pending and running queues.
 * Will be executed only as a single task and block all other tasks from execution.
 */
open class SingleStrategy : StateStrategy {

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
 * Same as SingleStrategy but will be added to the pending queue only if there are no pending or running task with a strategy with the same tag.
 */
open class SingleWithTagStrategy(private val tag: String) : SingleStrategy() {

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