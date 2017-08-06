/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.investing;

import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.function.Function;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.marketplaces.Marketplace;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.util.DaemonRuntimeExceptionHandler;

public class Investing implements Runnable {

    private final Investor.Builder builder;
    private final Authenticated authenticated;
    private final Refreshable<InvestmentStrategy> refreshableStrategy;
    private final Marketplace marketplace;
    private final TemporalAmount maximumSleepPeriod;
    private final ResultTracker buffer = new ResultTracker();

    public Investing(final Authenticated auth, final Investor.Builder builder, final Marketplace marketplace,
                     final Refreshable<InvestmentStrategy> strategy, final TemporalAmount maximumSleepPeriod) {
        this.authenticated = auth;
        this.builder = builder;
        this.refreshableStrategy = strategy;
        this.maximumSleepPeriod = maximumSleepPeriod;
        this.marketplace = marketplace;
        marketplace.registerListener((loans) -> {
            final Collection<LoanDescriptor> descriptors = buffer.acceptLoansFromMarketplace(loans);
            final Collection<Investment> result = getInvestor().apply(descriptors);
            buffer.acceptInvestmentsFromRobot(result);
        });
    }

    @Override
    public void run() {
        try {
            marketplace.run();
        } catch (final Throwable t) {
        /*
         * We catch Throwable so that we can inform users even about errors. Sudden death detection will take
         * care of errors stopping the thread.
         */
            new DaemonRuntimeExceptionHandler().handle(t);
        }
    }

    private Function<Collection<LoanDescriptor>, Collection<Investment>> getInvestor() {
        return new StrategyExecution(builder, refreshableStrategy, authenticated, maximumSleepPeriod);
    }
}
