/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.app.portfolio;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.common.remote.Zonky;

public class Portfolio {

    private final AtomicReference<PortfolioOverview> portfolioOverview = new AtomicReference<>();
    private final Statistics statistics;
    private final RemoteBalance balance;
    private final Supplier<BlockedAmountProcessor> blockedAmounts;

    private Portfolio(final Supplier<BlockedAmountProcessor> blockedAmounts, final Statistics statistics,
                      final Function<Portfolio, RemoteBalance> balance) {
        this.blockedAmounts = blockedAmounts;
        this.statistics = statistics;
        this.balance = balance.apply(this);
    }

    public static Portfolio create(final Tenant tenant, final Supplier<BlockedAmountProcessor> transfers) {
        return create(tenant, transfers, p -> RemoteBalance.create(tenant, p::balanceUpdated));
    }

    public static Portfolio create(final Tenant tenant, final Supplier<BlockedAmountProcessor> transfers,
                                   final Function<Portfolio, RemoteBalance> balance) {
        return new Portfolio(transfers, tenant.call(Zonky::getStatistics), balance);
    }

    public void simulateCharge(final int loanId, final Rating rating, final BigDecimal amount) {
        blockedAmounts.get().simulateCharge(loanId, rating, amount);
        balance.update(amount.negate()); // will result in balanceUpdated() getting called
    }

    public void balanceUpdated(final BigDecimal newBalance) {
        portfolioOverview.set(null); // reset overview, so that it could be recalculated on-demand
    }

    public RemoteBalance getRemoteBalance() {
        return balance;
    }

    public PortfolioOverview getOverview() {
        return portfolioOverview.updateAndGet(po -> {
            if (po == null) {
                return PortfolioOverview.calculate(balance.get(), statistics, blockedAmounts.get().getAdjustments(),
                                                   Delinquencies.getAmountsAtRisk());
            }
            return po;
        });
    }
}
