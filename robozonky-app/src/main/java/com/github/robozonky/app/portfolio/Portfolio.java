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
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.common.remote.Zonky;

public class Portfolio {

    private final Statistics statistics;
    private final RemoteBalance balance;
    private final Supplier<TransferMonitor> transfers;

    Portfolio(final Supplier<TransferMonitor> transfers, final Statistics statistics, final RemoteBalance balance) {
        this.transfers = transfers;
        this.statistics = statistics;
        this.balance = balance;
    }

    /**
     * Return a new instance of the class, loading information about all investments present and past from the Zonky
     * interface. This operation may take a while, as there may easily be hundreds or thousands of such investments.
     * @param tenant The API to be used to retrieve the data from Zonky.
     * @param transfers This will be initialized lazily as otherwise black-box system integration tests which test
     * the CLI would always end up calling Zonky and thus failing due to lack of authentication.
     * @return Empty in case there was a remote error.
     */
    public static Portfolio create(final Tenant tenant, final Supplier<TransferMonitor> transfers) {
        return new Portfolio(transfers, tenant.call(Zonky::getStatistics), RemoteBalance.create(tenant));
    }

    public static Portfolio create(final Tenant tenant, final Supplier<TransferMonitor> transfers,
                                   final RemoteBalance balance) {
        return new Portfolio(transfers, tenant.call(Zonky::getStatistics), balance);
    }

    public void simulateInvestment(final int loanId, final Rating rating, final BigDecimal amount) {
        transfers.get().simulateInvestment(loanId, rating, amount);
    }

    public void simulatePurchase(final int loanId, final Rating rating, final BigDecimal amount) {
        transfers.get().simulatePurchase(loanId, rating, amount);
    }

    Statistics getStatistics() {
        return statistics;
    }

    public RemoteBalance getRemoteBalance() {
        return balance;
    }

    public PortfolioOverview calculateOverview() {
        final BigDecimal actualBalance = balance.get().subtract(transfers.get().getUndetectedBlockedBalance());
        return PortfolioOverview.calculate(actualBalance, statistics, transfers.get().getAdjustments(),
                                           Delinquencies.getAmountsAtRisk());
    }
}