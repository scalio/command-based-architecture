package io.scal.commandbasedarchitecture.commands

/**
 * Base class for all commands.
 * This command should implement all filtering by itself.
 */
abstract class Command<CommandResult, DataState : Any?>(
    val strategy: ExecutionStrategy
) {

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
     * @see Command.onExecuteSuccess
     * @see Command.onExecuteFail
     */
    open suspend fun executeCommand(dataState: DataState): CommandResult {
        throw IllegalStateException("executeCommand or executeCommandWithSideEffects should be implemented")
    }

    open suspend fun executeCommandWithSideEffects(
        getCurrentDataState: () -> DataState,
        updateCurrentDataState: (DataState) -> Unit,
    ): CommandResult =
        executeCommand(getCurrentDataState())

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
    open fun shouldAddToPendingCommands(
        pendingActionCommands: List<Command<*, *>>,
        runningActionCommands: List<Command<*, *>>
    ): Boolean =
        strategy.shouldAddToPendingActions(pendingActionCommands, runningActionCommands)

    /**
     * Method to control other tasks.
     * Will be called only if current task is in execution state and pendingActionCommand needs to be executed immediately.
     *
     * This method may be called multiple times
     *
     * @return true if pendingActionCommand should wait some time (usually for current command execution finish), false if other command can be executed in parallel mode
     */
    open fun shouldBlockOtherCommand(pendingActionCommand: Command<*, *>): Boolean =
        strategy.shouldBlockOtherTask(pendingActionCommand)

    /**
     * Method to control that current command is able to execute immediately.
     *
     * This method may be called multiple times
     *
     * @return true if current command is able execute immediately, false - if command should wait some time
     */
    open fun shouldExecute(
        pendingActionCommands: List<Command<*, *>>,
        runningActionCommands: List<Command<*, *>>
    ): Boolean =
        strategy.shouldExecuteAction(pendingActionCommands, runningActionCommands)
}