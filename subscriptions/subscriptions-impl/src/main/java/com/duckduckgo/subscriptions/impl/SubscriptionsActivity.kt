/*
 * Copyright (c) 2023 DuckDuckGo
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

package com.duckduckgo.subscriptions.impl

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.ProductDetails
import com.duckduckgo.anvil.annotations.ContributeToActivityStarter
import com.duckduckgo.anvil.annotations.InjectWith
import com.duckduckgo.app.global.DuckDuckGoActivity
import com.duckduckgo.di.scopes.ActivityScope
import com.duckduckgo.mobile.android.ui.viewbinding.viewBinding
import com.duckduckgo.navigation.api.GlobalActivityStarter
import com.duckduckgo.subscriptions.impl.SubscriptionsActivity.Companion.SubscriptionsScreenWithEmptyParams
import com.duckduckgo.subscriptions.impl.SubscriptionsViewModel.Command
import com.duckduckgo.subscriptions.impl.SubscriptionsViewModel.Command.ErrorMessage
import com.duckduckgo.subscriptions.impl.SubscriptionsViewModel.ViewState
import com.duckduckgo.subscriptions.impl.billing.getPrice
import com.duckduckgo.subscriptions.impl.databinding.ActivitySubscriptionsBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@InjectWith(ActivityScope::class)
@ContributeToActivityStarter(SubscriptionsScreenWithEmptyParams::class)
class SubscriptionsActivity : DuckDuckGoActivity() {

    private val viewModel: SubscriptionsViewModel by bindViewModel()
    private val binding: ActivitySubscriptionsBinding by viewBinding()

    private val toolbar
        get() = binding.includeToolbar.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.subscriptionsFlow.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).onEach {
            it.subscriptionDetails?.let { subscriptionDetails ->
                if (it.hasSubscription == true) {
                    renderSubscribed(subscriptionDetails)
                } else {
                    renderNotSubscribed()
                }
                renderProducts(it)
            }
        }.launchIn(lifecycleScope)

        viewModel.commands()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach { processCommand(it) }
            .launchIn(lifecycleScope)

        setContentView(binding.root)
        setupToolbar(toolbar)

        binding.recoverSubscriptionButton.setOnClickListener {
            viewModel.recoverSubscription()
        }
    }

    private fun processCommand(command: Command) {
        if (command is ErrorMessage) {
            Toast.makeText(this, command.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun renderNotSubscribed() {
        binding.purchaseDetails.text = "You are not subscribed yet"
    }
    private fun renderSubscribed(productDetails: ProductDetails) {
        binding.purchaseDetails.text = "You are subscribed to ${productDetails.name}"
    }

    private fun renderProducts(state: ViewState) {
        val yearly = state.yearlySubscription
        val monthly = state.monthlySubscription
        val uk = state.ukSubscription
        val ne = state.netherlandsSubscription
        val productDetails = state.subscriptionDetails!!
        binding.description.text = productDetails.description
        binding.name.text = productDetails.name
        yearly?.let {
            binding.buyButtonPlan1.apply {
                text = yearly.getPrice()
                setOnClickListener {
                    viewModel.buySubscription(this@SubscriptionsActivity, productDetails, yearly.offerToken)
                }
            }
        }
        monthly?.let {
            binding.buyButtonPlan2.apply {
                text = monthly.getPrice()
                setOnClickListener {
                    viewModel.buySubscription(this@SubscriptionsActivity, productDetails, monthly.offerToken)
                }
            }
            binding.resetAccountButton.apply {
                setOnClickListener {
                    viewModel.buySubscription(this@SubscriptionsActivity, productDetails, monthly.offerToken, isReset = true)
                }
            }
        }
        uk?.let {
            binding.buyButtonPlan3.apply {
                text = uk.getPrice()
                setOnClickListener {
                    viewModel.buySubscription(this@SubscriptionsActivity, productDetails, uk.offerToken)
                }
            }
        }
        ne?.let {
            binding.buyButtonPlan4.apply {
                text = ne.getPrice()
                setOnClickListener {
                    viewModel.buySubscription(this@SubscriptionsActivity, productDetails, ne.offerToken)
                }
            }
        }
    }

    companion object {
        object SubscriptionsScreenWithEmptyParams : GlobalActivityStarter.ActivityParams
    }
}
