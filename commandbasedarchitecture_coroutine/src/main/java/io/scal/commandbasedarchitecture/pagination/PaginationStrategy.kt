package io.scal.commandbasedarchitecture.pagination

import io.scal.commandbasedarchitecture.SingleStrategy
import io.scal.commandbasedarchitecture.SingleWithTagStrategy

open class RefreshStrategy : SingleWithTagStrategy("RefreshStrategy")

open class LoadNextStrategy : SingleStrategy()