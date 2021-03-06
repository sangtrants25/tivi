/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.banes.chris.tivi.home.discover

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_summary.*
import me.banes.chris.tivi.R
import me.banes.chris.tivi.data.Entry
import me.banes.chris.tivi.data.entities.ListItem
import me.banes.chris.tivi.data.entities.PopularEntry
import me.banes.chris.tivi.data.entities.TrendingEntry
import me.banes.chris.tivi.extensions.observeK
import me.banes.chris.tivi.home.HomeFragment
import me.banes.chris.tivi.home.HomeNavigator
import me.banes.chris.tivi.home.HomeNavigatorViewModel
import me.banes.chris.tivi.ui.ListItemSharedElementHelper
import me.banes.chris.tivi.ui.SpacingItemDecorator
import me.banes.chris.tivi.util.GridToGridTransitioner

internal class DiscoverFragment : HomeFragment<DiscoverViewModel>() {
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var homeNavigator: HomeNavigator

    private val controller = DiscoverEpoxyController(object : DiscoverEpoxyController.Callbacks {
        private val listItemSharedElementHelper by lazy(LazyThreadSafetyMode.NONE) {
            ListItemSharedElementHelper(summary_rv)
        }

        override fun onTrendingHeaderClicked(items: List<ListItem<TrendingEntry>>?) {
            viewModel.onTrendingHeaderClicked(homeNavigator, listItemSharedElementHelper.createForItems(items))
        }

        override fun onPopularHeaderClicked(items: List<ListItem<PopularEntry>>?) {
            viewModel.onPopularHeaderClicked(homeNavigator, listItemSharedElementHelper.createForItems(items))
        }

        override fun onItemClicked(item: ListItem<out Entry>) {
            viewModel.onItemPostedClicked(homeNavigator, item.show!!,
                    listItemSharedElementHelper.createForItem(item, "poster"))
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(DiscoverViewModel::class.java)
        homeNavigator = ViewModelProviders.of(activity!!, viewModelFactory).get(HomeNavigatorViewModel::class.java)

        GridToGridTransitioner.setupFirstFragment(this,
                intArrayOf(R.id.summary_appbarlayout, R.id.summary_status_scrim))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.data.observeK(this) { model ->
            controller.setData(model?.trendingItems, model?.popularItems, model?.tmdbImageUrlProvider)
            scheduleStartPostponedTransitions()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()

        // Setup span and columns
        gridLayoutManager = summary_rv.layoutManager as GridLayoutManager
        gridLayoutManager.spanSizeLookup = controller.spanSizeLookup
        controller.spanCount = gridLayoutManager.spanCount

        summary_rv.apply {
            adapter = controller.adapter
            addItemDecoration(SpacingItemDecorator(paddingLeft))
        }

        summary_toolbar.apply {
            title = getString(R.string.discover_title)
            inflateMenu(R.menu.home_toolbar)
            setOnMenuItemClickListener {
                onMenuItemClicked(it)
            }
        }
    }

    override fun getMenu(): Menu? = summary_toolbar.menu

    internal fun scrollToTop() {
        summary_rv.apply {
            stopScroll()
            smoothScrollToPosition(0)
        }
        summary_appbarlayout.setExpanded(true)
    }
}
