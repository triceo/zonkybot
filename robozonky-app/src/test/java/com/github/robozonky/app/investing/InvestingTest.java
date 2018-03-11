/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.investing;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvestingTest extends AbstractZonkyLeveragingTest {

    private static final InvestmentStrategy NONE_ACCEPTING_STRATEGY = (a, p, r) -> Stream.empty(),
            ALL_ACCEPTING_STRATEGY = (a, p, r) -> a.stream().map(d -> d.recommend(200).get());
    private static final Supplier<Optional<InvestmentStrategy>> NONE_ACCEPTING =
            () -> Optional.of(NONE_ACCEPTING_STRATEGY),
            ALL_ACCEPTING = () -> Optional.of(ALL_ACCEPTING_STRATEGY);

    @Test
    void noStrategy() {
        final int loanId = (int) (Math.random() * 1000); // new ID every time to avoid caches
        final Loan loan = Loan.custom()
                .setId(loanId)
                .setAmount(2)
                .build();
        final LoanDescriptor ld = new LoanDescriptor(loan);
        final Investing exec = new Investing(null, Optional::empty, null, Duration.ofMinutes(60));
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(1000);
        final Portfolio portfolio = Portfolio.create(z, mockBalance(z));
        assertThat(exec.apply(portfolio, Collections.singletonList(ld))).isEmpty();
        // check events
        final List<Event> events = this.getNewEvents();
        assertThat(events).isEmpty();
    }

    @Test
    void noItems() {
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(1000);
        final Portfolio portfolio = Portfolio.create(z, mockBalance(z));
        final Investor.Builder builder = new Investor.Builder().asDryRun();
        final Authenticated auth = mock(Authenticated.class);
        when(auth.call(isNotNull())).thenAnswer(invocation -> {
            final Function<Zonky, Collection<RawInvestment>> f = invocation.getArgument(0);
            return f.apply(z);
        });
        final Investing exec = new Investing(builder, ALL_ACCEPTING, auth, Duration.ofMinutes(60));
        assertThat(exec.apply(portfolio, Collections.emptyList())).isEmpty();
    }

    @Test
    void noneAccepted() {
        final int loanId = (int) (Math.random() * 1000); // new ID every time to avoid caches
        final Loan loan = Loan.custom()
                .setId(loanId)
                .setAmount(100_000)
                .setRating(Rating.D)
                .setMyInvestment(mockMyInvestment())
                .setDatePublished(OffsetDateTime.now())
                .build();
        final LoanDescriptor ld = new LoanDescriptor(loan);
        final Investor.Builder builder = new Investor.Builder().asDryRun();
        final Zonky z = harmlessZonky(9000);
        final Portfolio portfolio = Portfolio.create(z, mockBalance(z));
        when(z.getLoan(eq(loanId))).thenReturn(loan);
        final Authenticated auth = mock(Authenticated.class);
        when(auth.call(isNotNull())).thenAnswer(invocation -> {
            final Function<Zonky, Collection<RawInvestment>> f = invocation.getArgument(0);
            return f.apply(z);
        });
        final Investing exec = new Investing(builder, NONE_ACCEPTING, auth, Duration.ofMinutes(60));
        assertThat(exec.apply(portfolio, Collections.singleton(ld))).isEmpty();
    }

    @Test
    void someAccepted() {
        final int loanId = (int) (Math.random() * 1000); // new ID every time to avoid caches
        final Loan loan = Loan.custom()
                .setId(loanId)
                .setAmount(100_000)
                .setRating(Rating.D)
                .setRemainingInvestment(20_000)
                .setDatePublished(OffsetDateTime.now())
                .build();
        final LoanDescriptor ld = new LoanDescriptor(loan);
        final Investor.Builder builder = new Investor.Builder().asDryRun();
        final Zonky z = harmlessZonky(10_000);
        when(z.getLoan(eq(loanId))).thenReturn(loan);
        final Portfolio portfolio = Portfolio.create(z, mockBalance(z));
        final Authenticated auth = mockAuthentication(z);
        final Investing exec = new Investing(builder, ALL_ACCEPTING, auth, Duration.ZERO);
        final Collection<Investment> result = exec.apply(portfolio, Collections.singleton(ld));
        verify(z, never()).invest(any()); // dry run
        assertThat(result)
                .extracting(Investment::getLoanId)
                .isEqualTo(Collections.singletonList(loanId));
    }
}
