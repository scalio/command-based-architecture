package io.scal.commandbasedarchitecture.sample_coroutine.ui.main

import io.scal.commandbasedarchitecture.pagination.PageDataWithNextPageNumber
import io.scal.commandbasedarchitecture.pagination.PaginationState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIItem

typealias MainScreenState = PaginationState<UIItem, UIMainItem, PageDataWithNextPageNumber<UIMainItem>>