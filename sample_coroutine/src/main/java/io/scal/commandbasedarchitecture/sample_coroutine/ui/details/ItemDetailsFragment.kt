package io.scal.commandbasedarchitecture.sample_coroutine.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.scal.commandbasedarchitecture.sample_coroutine.R
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.model.UIProgressErrorItem
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view.handleProgressErrorState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.base.view.showNoProgressErrorState
import io.scal.commandbasedarchitecture.sample_coroutine.ui.root.RootActivity
import kotlinx.android.synthetic.main.fragment_details.*

class ItemDetailsFragment : Fragment() {

    private val simpleViewModelInstance: DetailsViewModel by viewModels<SimpleDetailsViewModel>()
    private val broadCastViewModelInstance: DetailsViewModel by viewModels<BroadcastDetailsViewModel>()

    @Suppress("ConstantConditionIf")
    private val viewModelInstance: DetailsViewModel by lazy {
        if (RootActivity.userBroadcastViewModels) broadCastViewModelInstance else simpleViewModelInstance
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_details, container!!, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemUid = arguments?.getString("itemUid")
        if (null == itemUid) {
            view.post { activity?.onBackPressed() }
        } else {
            viewModelInstance.itemUid = itemUid
            viewModelInstance.reload()

            initSwipeToRefreshView()
            initStateModel()
        }
    }

    private fun initSwipeToRefreshView() {
        srlData.setOnRefreshListener { viewModelInstance.reload() }
    }

    private fun initStateModel() {
        viewModelInstance.dataState
            .observe(
                viewLifecycleOwner,
                Observer { dataState ->
                    if (null == dataState) return@Observer

                    when (val item = dataState.item) {
                        null -> {
                            if (dataState.refreshStatus is UIProgressErrorItem.Progress) {
                                showNoProgressErrorState(progressError)
                            } else {
                                dataState.refreshStatus.handleProgressErrorState(progressError)
                            }
                            content.visibility = View.GONE
                        }
                        else -> {
                            showNoProgressErrorState(progressError)
                            content.visibility = View.VISIBLE

                            tvTitle.text = item.title

                            if (item.favoriteState.favorite) {
                                addToFavorite.visibility = View.GONE
                                removeFromFavorite
                                    .apply {
                                        visibility = View.VISIBLE
                                        setOnClickListener {
                                            viewModelInstance.removeFromFavorite(item)
                                        }
                                    }
                            } else {
                                removeFromFavorite.visibility = View.GONE
                                addToFavorite
                                    .apply {
                                        visibility = View.VISIBLE
                                        setOnClickListener {
                                            viewModelInstance.addToFavorite(item)
                                        }
                                    }
                            }
                        }
                    }

                    srlData.isRefreshing = dataState.refreshStatus is UIProgressErrorItem.Progress
                }
            )
    }

    companion object {

        fun createScreen(itemUid: String): Fragment =
            ItemDetailsFragment()
                .apply {
                    arguments = Bundle()
                        .apply { putString("itemUid", itemUid) }
                }
    }
}