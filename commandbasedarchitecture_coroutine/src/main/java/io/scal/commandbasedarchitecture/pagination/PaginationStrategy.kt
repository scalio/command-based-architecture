package io.scal.commandbasedarchitecture.pagination

import io.scal.commandbasedarchitecture.ActionCommand
import io.scal.commandbasedarchitecture.RemoveOnlyList
import io.scal.commandbasedarchitecture.SingleWithTagStrategy

/**
 * Base class for Refresh actions.
 * This is single strategy with tag that means it blocks every other action if refresh is running and no more then one refresh action can be added to a quire.
 */
open class RefreshStrategy : SingleWithTagStrategy("RefreshStrategy")

/**
 * Base class for Loading Next actions.
 * This is single strategy with tag that means it blocks every other action if this command is running and no more then one loading next action can be added to a quire.
 * Also in addition this strategy drop any loading next page if refresh action is waiting or executing.
 */
open class LoadNextStrategy : SingleWithTagStrategy("LoadNext") {

    override fun shouldAddToPendingActions(
        pendingActionCommands: RemoveOnlyList<ActionCommand<*, *>>,
        runningActionCommands: List<ActionCommand<*, *>>
    ): Boolean =
        super.shouldAddToPendingActions(pendingActionCommands, runningActionCommands)
                && !pendingActionCommands.any { it.strategy is RefreshStrategy }
                && !runningActionCommands.any { it.strategy is RefreshStrategy }
}