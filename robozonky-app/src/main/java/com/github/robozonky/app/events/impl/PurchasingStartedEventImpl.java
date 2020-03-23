/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.app.events.impl;

import java.util.StringJoiner;

import com.github.robozonky.api.notifications.PurchasingStartedEvent;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class PurchasingStartedEventImpl extends AbstractEventImpl implements PurchasingStartedEvent {

    private final PortfolioOverview portfolioOverview;

    public PurchasingStartedEventImpl(final PortfolioOverview portfolio) {
        this.portfolioOverview = portfolio;
    }

    @Override
    public PortfolioOverview getPortfolioOverview() {
        return portfolioOverview;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PurchasingStartedEventImpl.class.getSimpleName() + "[", "]")
            .add("super=" + super.toString())
            .add("portfolioOverview=" + portfolioOverview)
            .toString();
    }
}
