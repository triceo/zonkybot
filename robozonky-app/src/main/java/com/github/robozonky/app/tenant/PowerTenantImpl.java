/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.app.tenant;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.SessionEvent;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.internal.SessionInfoImpl;
import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.state.InstanceState;
import com.github.robozonky.internal.state.TenantState;
import com.github.robozonky.internal.tenant.Availability;
import com.github.robozonky.internal.tenant.LazyEvent;
import com.github.robozonky.internal.tenant.RemotePortfolio;
import com.github.robozonky.internal.util.functional.Memoizer;

class PowerTenantImpl implements PowerTenant {

    private static final Logger LOGGER = LogManager.getLogger(PowerTenantImpl.class);

    private final SessionInfo sessionInfo;
    private final ApiProvider apis;
    private final RemotePortfolio portfolio;
    private final ZonkyApiTokenSupplier token;
    private final StrategyProvider strategyProvider;
    private final Supplier<Cache<Loan>> loanCache = Memoizer.memoize(() -> Cache.forLoan(this));
    private final StatefulBoundedBalance balance;
    private final Supplier<Availability> availability;

    PowerTenantImpl(final String username, final String sessionName, final boolean isDryRun, final ApiProvider apis,
            final StrategyProvider strategyProvider, final ZonkyApiTokenSupplier tokenSupplier) {
        this.strategyProvider = strategyProvider;
        this.apis = apis;
        this.sessionInfo = new SessionInfoImpl(() -> call(Zonky::getConsents), () -> call(Zonky::getRestrictions),
                username, sessionName, isDryRun);
        this.token = tokenSupplier;
        this.availability = Memoizer.memoize(() -> new AvailabilityImpl(token, apis.getMeteredRequestTimer()));
        this.portfolio = new RemotePortfolioImpl(this);
        this.balance = new StatefulBoundedBalance(this);
    }

    @Override
    public <T> T call(final Function<Zonky, T> operation) {
        return apis.call(operation, token);
    }

    @Override
    public Availability getAvailability() {
        return availability.get();
    }

    @Override
    public RemotePortfolio getPortfolio() {
        return portfolio;
    }

    @Override
    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    @Override
    public Optional<InvestmentStrategy> getInvestmentStrategy() {
        return strategyProvider.getToInvest();
    }

    @Override
    public Optional<SellStrategy> getSellStrategy() {
        return strategyProvider.getToSell();
    }

    @Override
    public Optional<PurchaseStrategy> getPurchaseStrategy() {
        return strategyProvider.getToPurchase();
    }

    @Override
    public Optional<ReservationStrategy> getReservationStrategy() {
        return strategyProvider.getForReservations();
    }

    @Override
    public Loan getLoan(final int loanId) {
        return loanCache.get()
            .get(loanId);
    }

    @Override
    public <T> InstanceState<T> getState(final Class<T> clz) {
        return TenantState.of(getSessionInfo())
            .in(clz);
    }

    @Override
    public void close() {
        try {
            token.close();
        } catch (final Exception ex) {
            LOGGER.debug("Failed closing tenant {}.", this, ex);
        }
    }

    @Override
    public Money getKnownBalanceUpperBound() {
        return balance.get();
    }

    @Override
    public void setKnownBalanceUpperBound(final Money knownBalanceUpperBound) {
        balance.set(knownBalanceUpperBound);
    }

    @Override
    public CompletableFuture<?> fire(final SessionEvent event) {
        return Events.forSession(this)
            .fire(event);
    }

    @Override
    public CompletableFuture<?> fire(final LazyEvent<? extends SessionEvent> event) {
        return Events.forSession(this)
            .fire(event);
    }

    @Override
    public String toString() {
        return "PowerTenantImpl{" +
                "sessionInfo=" + sessionInfo +
                '}';
    }
}
