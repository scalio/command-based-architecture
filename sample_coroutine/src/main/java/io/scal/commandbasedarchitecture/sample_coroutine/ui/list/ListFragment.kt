package io.scal.commandbasedarchitecture.sample_coroutine.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.scal.commandbasedarchitecture.pagination.dataAndNextPageLoadingStatus
import io.scal.commandbasedarchitecture.sample_coroutine.R
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIProgressErrorItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view.handleProgressErrorState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view.listenForEndScroll
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view.showNoProgressErrorState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.details.ItemDetailsFragment
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.adapter.ListAdapter
import io.scal.commandbasedarchitecture.sample_coroutine.ui.root.RootActivity
import kotlinx.android.synthetic.main.fragment_list.*

class ListFragment : Fragment() {

    private val simpleViewModelInstance: ListViewModel by viewModels<SimpleListViewModel>()
    private val broadCastViewModelInstance: ListViewModel by viewModels<BroadcastListViewModel>()

    @Suppress("ConstantConditionIf")
    private val viewModelInstance: ListViewModel by lazy {
        if (RootActivity.userBroadcastViewModels) broadCastViewModelInstance else simpleViewModelInstance
    }

    private val adapter by lazy {
        ListAdapter(viewModelInstance) {
            navigateToItemDetails(it, (activity as? RootActivity) ?: return@ListAdapter)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_list, container!!, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
        initStateModel()
    }

    private fun navigateToItemDetails(uiMainItem: UIMainItem, rootActivity: RootActivity) {
        rootActivity.addNewFragment(ItemDetailsFragment.createScreen(uiMainItem.uid))
    }

    private fun initRecyclerView() {
        rvData.setHasFixedSize(true)
        rvData.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        rvData.adapter = adapter

        srlData.setOnRefreshListener { viewModelInstance.reload() }

        // simple realization for next page loading trigger
        rvData.listenForEndScroll(4) { viewModelInstance.loadNextPage() }
    }

    private fun initStateModel() {
        viewModelInstance.dataState
            .observe(
                viewLifecycleOwner,
                Observer { dataState ->
                    if (null == dataState) return@Observer

                    val items = dataState.dataAndNextPageLoadingStatus
                    when {
                        // we do not have any data or next page loading status -> empty loading or empty error state
                        items == null -> {
                            emptyData.visibility = View.GONE
                            if (dataState.refreshStatus is UIProgressErrorItem.Progress) {
                                // we do not want double progress indicator for loading, we just use STR all the time
                                showNoProgressErrorState(progressError)
                            } else {
                                dataState.refreshStatus.handleProgressErrorState(progressError)
                            }
                            adapter.releaseData()
                        }
                        // we do not have any data -> empty data state
                        items.isEmpty() -> {
                            emptyData.visibility = View.VISIBLE
                            showNoProgressErrorState(progressError)
                            adapter.releaseData()
                        }
                        // we have some data to show -> data state
                        else -> {
                            emptyData.visibility = View.GONE
                            showNoProgressErrorState(progressError)
                            adapter.setupData(items)
                        }
                    }

                    // we always show STR progress if any
                    srlData.isRefreshing = dataState.refreshStatus is UIProgressErrorItem.Progress
                }
            )
    }
}