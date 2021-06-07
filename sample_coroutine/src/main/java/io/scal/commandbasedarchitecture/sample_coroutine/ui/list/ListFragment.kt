package io.scal.commandbasedarchitecture.sample_coroutine.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.scal.commandbasedarchitecture.model.dataAndNextPageLoadingStatus
import io.scal.commandbasedarchitecture.sample_coroutine.databinding.FragmentListBinding
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIProgressErrorItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view.handleProgressErrorState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view.listenForEndScroll
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view.showNoProgressErrorState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.details.ItemDetailsFragment
import io.scal.commandbasedarchitecture.sample_coroutine.ui.list.adapter.ListAdapter
import io.scal.commandbasedarchitecture.sample_coroutine.ui.root.RootActivity

class ListFragment : Fragment() {

    private val simpleViewModelInstance: ListViewModel by viewModels<SimpleListViewModel>()

    private val viewModelInstance: ListViewModel by lazy { simpleViewModelInstance }

    private val adapter by lazy {
        ListAdapter(viewModelInstance)
    }

    private var binding: FragmentListBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListBinding.inflate(inflater, container!!, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val readBinding = binding!!

        initRecyclerView(readBinding)
        initStateModel(readBinding)
    }

    private fun navigateToItemDetails(uiMainItem: UIMainItem, rootActivity: RootActivity) {
        rootActivity.addNewFragment(ItemDetailsFragment.createScreen(uiMainItem.uid))
    }

    private fun initRecyclerView(readBinding: FragmentListBinding) {
        readBinding.rvData.setHasFixedSize(true)
        readBinding.rvData.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        readBinding.rvData.adapter = adapter

        readBinding.srlData.setOnRefreshListener { viewModelInstance.reload() }

        // simple realization for next page loading trigger
        readBinding.rvData.listenForEndScroll(4) { viewModelInstance.loadNextPage() }
    }

    private fun initStateModel(readBinding: FragmentListBinding) {
        viewModelInstance.screenState
            .observe(
                viewLifecycleOwner,
                Observer { dataState ->
                    if (null == dataState) return@Observer

                    val items = dataState.dataAndNextPageLoadingStatus
                    when {
                        // we do not have any data or next page loading status -> empty loading or empty error state
                        items == null -> {
                            readBinding.emptyData.visibility = View.GONE
                            if (dataState.refreshStatus is UIProgressErrorItem.Progress) {
                                // we do not want double progress indicator for loading, we just use STR all the time
                                showNoProgressErrorState(readBinding.progressError)
                            } else {
                                dataState.refreshStatus.handleProgressErrorState(readBinding.progressError)
                            }
                            adapter.releaseData()
                        }
                        // we do not have any data -> empty data state
                        items.isEmpty() -> {
                            readBinding.emptyData.visibility = View.VISIBLE
                            showNoProgressErrorState(readBinding.progressError)
                            adapter.releaseData()
                        }
                        // we have some data to show -> data state
                        else -> {
                            readBinding.emptyData.visibility = View.GONE
                            showNoProgressErrorState(readBinding.progressError)
                            adapter.setupData(items)
                        }
                    }

                    // we always show STR progress if any
                    readBinding.srlData.isRefreshing = dataState.refreshStatus is UIProgressErrorItem.Progress
                }
            )
    }
}