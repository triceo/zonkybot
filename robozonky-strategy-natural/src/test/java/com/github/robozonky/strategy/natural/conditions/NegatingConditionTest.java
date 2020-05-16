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

package com.github.robozonky.strategy.natural.conditions;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.strategy.natural.wrappers.Wrapper;
import com.github.robozonky.test.mock.MockLoanBuilder;

class NegatingConditionTest {

    private static final PortfolioOverview FOLIO = mock(PortfolioOverview.class);

    @Test
    void doubleNegation() {
        final MarketplaceFilterCondition c = item -> true;
        assertThat(c.negate()
            .negate()).isSameAs(c);
        assertThat(c.mayRequireRemoteRequests()).isFalse();
    }

    @Test
    void negatingTrue() {
        final MarketplaceFilterCondition c = MarketplaceFilterCondition.alwaysAccepting();
        final NegatingCondition nc = new NegatingCondition(c);
        assertThat(nc.test(Wrapper.wrap(new LoanDescriptor(MockLoanBuilder.fresh()), FOLIO))).isFalse();
    }

    @Test
    void negatingFalse() {
        final MarketplaceFilterCondition c = MarketplaceFilterCondition.neverAccepting();
        final NegatingCondition nc = new NegatingCondition(c);
        assertThat(nc.test(Wrapper.wrap(new LoanDescriptor(MockLoanBuilder.fresh()), FOLIO))).isTrue();
    }
}
