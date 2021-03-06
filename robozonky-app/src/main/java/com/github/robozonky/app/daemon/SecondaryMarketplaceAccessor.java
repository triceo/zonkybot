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

package com.github.robozonky.app.daemon;

import java.time.Duration;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.LastPublishedItem;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.Settings;
import com.github.robozonky.internal.remote.Select;
import com.github.robozonky.internal.remote.Zonky;

final class SecondaryMarketplaceAccessor extends AbstractMarketplaceAccessor<ParticipationDescriptor> {

    private static final Duration FULL_CHECK_INTERVAL = Duration.ofHours(1);
    private static final Logger LOGGER = Audit.purchasing();

    private final PowerTenant tenant;
    private final UnaryOperator<LastPublishedItem> stateAccessor;

    public SecondaryMarketplaceAccessor(final PowerTenant tenant,
            final UnaryOperator<LastPublishedItem> stateAccessor) {
        super(LOGGER);
        this.tenant = tenant;
        this.stateAccessor = stateAccessor;
    }

    @Override
    protected OptionalInt getMaximumItemsToRead() {
        var max = Settings.INSTANCE.getMaxItemsReadFromSecondaryMarketplace();
        return sanitizeMaximumItemCount(max);
    }

    @Override
    protected Select getBaseFilter() {
        Money upperBalanceBound = tenant.getKnownBalanceUpperBound();
        Money maximumInvestmentAmount = tenant.getSessionInfo()
            .getMaximumInvestmentAmount();
        Money limit = upperBalanceBound.min(maximumInvestmentAmount);
        return new Select()
            .equalsPlain("willNotExceedLoanInvestmentLimit", "true")
            .greaterThanOrEquals("remainingPrincipal", 2) // Ignore near-0 participation clutter.
            .lessThanOrEquals("remainingPrincipal", limit.getValue()
                .intValue());
    }

    @Override
    public Duration getForcedMarketplaceCheckInterval() {
        return FULL_CHECK_INTERVAL;
    }

    @Override
    public Stream<ParticipationDescriptor> getMarketplace() {
        var cache = SoldParticipationCache.forTenant(tenant);
        /*
         * Do not make this parallel.
         * Each participation will be processed individually and if done in parallel, the portfolio structure could go
         * haywire.
         * The code is designed to purchase, rebuild the portfolio structure, and then purchase again.
         */
        var participations = tenant.call(zonky -> zonky.getAvailableParticipations(getIncrementalFilter()))
            .filter(p -> { // never re-purchase what was once sold
                var loanId = p.getLoanId();
                if (cache.wasOnceSold(p.getInvestmentId())) {
                    LOGGER.debug("Loan #{} already sold before, ignoring.", loanId);
                    return false;
                } else {
                    return true;
                }
            });
        if (getMaximumItemsToRead().isPresent()) {
            var limit = getMaximumItemsToRead().orElseThrow();
            LOGGER.trace("Enforcing read limit of {} latest items.", limit);
            participations = participations.limit(limit);
        }
        return participations.map(p -> {
            var loanId = p.getLoanId();
            return new ParticipationDescriptor(p, () -> tenant.getLoan(loanId),
                    () -> tenant.call(a -> a.getParticipationDetail(loanId)));
        });
    }

    @Override
    public boolean hasUpdates() {
        try {
            var current = tenant.call(Zonky::getLastPublishedParticipationInfo);
            var previous = stateAccessor.apply(current);
            LOGGER.trace("Current is {}, previous is {}.", current, previous);
            return !Objects.equals(previous, current);
        } catch (final Exception ex) {
            LOGGER.debug("Zonky secondary marketplace status endpoint failed, forcing live marketplace check.", ex);
            return true;
        }
    }

}
