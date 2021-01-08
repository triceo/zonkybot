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

package com.github.robozonky.strategy.natural.conditions;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.robozonky.strategy.natural.wrappers.Wrapper;

class RemainingInterestConditionTest {

    @Test
    void lessThan() {
        final MarketplaceFilterCondition condition = RemainingInterestCondition.lessThan(1);
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getRemainingInterest()).thenReturn(Optional.of(BigDecimal.ZERO));
        assertThat(condition).accepts(w);
        when(w.getRemainingInterest()).thenReturn(Optional.of(BigDecimal.ONE));
        assertThat(condition).rejects(w);
    }

    @Test
    void moreThan() {
        final MarketplaceFilterCondition condition = RemainingInterestCondition.moreThan(0);
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getRemainingInterest()).thenReturn(Optional.of(BigDecimal.ZERO));
        assertThat(condition).rejects(w);
        when(w.getRemainingInterest()).thenReturn(Optional.of(BigDecimal.ONE));
        assertThat(condition).accepts(w);
    }

    @Test
    void exact() {
        final MarketplaceFilterCondition condition = RemainingInterestCondition.exact(0, 1);
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getRemainingInterest()).thenReturn(Optional.of(BigDecimal.ZERO));
        assertThat(condition).accepts(w);
        when(w.getRemainingInterest()).thenReturn(Optional.of(BigDecimal.ONE));
        assertThat(condition).accepts(w);
        when(w.getRemainingInterest()).thenReturn(Optional.of(BigDecimal.TEN));
        assertThat(condition).rejects(w);
    }
}