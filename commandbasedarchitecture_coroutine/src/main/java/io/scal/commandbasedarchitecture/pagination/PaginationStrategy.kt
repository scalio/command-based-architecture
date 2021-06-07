package io.scal.commandbasedarchitecture.pagination

import io.scal.commandbasedarchitecture.commands.Command
import io.scal.commandbasedarchitecture.commands.SingleWithTagStrategy
import io.scal.commandbasedarchitecture.model.RemoveOnlyList

/**
 * Base class for Refresh actions.
 * This is a single strategy with tag that means it blocks every other action if refresh is running
 * and no more than one refresh action can be added to a queue.
 */
open class RefreshStrategy(tag: String = "RefreshStrategy") : SingleWithTagStrategy(tag)

/**
 * Base class for Loading Next actions.
 * This is a single strategy with tag that means it blocks every other action if this command is running
 * and no more than one loading next action can be added to a queue.
 * In addition, this strategy drops any loading next page if refresh action is waiting or executing.
 */
open class LoadNextStrategy(tag: String = "LoadNext") : SingleWithTagStrategy(tag) {

    override fun shouldAddToPendingActions(
        pendingActionCommands: RemoveOnlyList<Command<*, *>>,
        runningActionCommands: List<Command<*, *>>
    ): Boolean =
        super.shouldAddToPendingActions(pendingActionCommands, runningActionCommands)
                && !pendingActionCommands.any { it.strategy is RefreshStrategy }
                && !runningActionCommands.any { it.strategy is RefreshStrategy }
}