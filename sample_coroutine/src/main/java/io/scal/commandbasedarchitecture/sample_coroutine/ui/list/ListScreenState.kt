package io.scal.commandbasedarchitecture.sample_coroutine.ui.list

import io.scal.commandbasedarchitecture.model.PageDataWithNextPageNumber
import io.scal.commandbasedarchitecture.model.PaginationState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIItem

typealias ListScreenState = PaginationState<UIItem, UIMainItem, PageDataWithNextPageNumber<UIMainItem>>