package io.scal.commandbasedarchitecture.sample_coroutine.ui.list

import io.scal.commandbasedarchitecture.pagination.PageDataWithNextPageNumber
import io.scal.commandbasedarchitecture.pagination.PaginationState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIItem

typealias ListScreenState = PaginationState<UIItem, UIMainItem, PageDataWithNextPageNumber<UIMainItem>>