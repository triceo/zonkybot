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

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.strategy.natural.wrappers.Wrapper;

class RelativeDiscountConditionTest {

    @Test
    void lessThan() {
        final MarketplaceFilterCondition condition = RelativeDiscountCondition.lessThan(Ratio.fromPercentage(10));
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getDiscount()).thenReturn(Optional.of(BigDecimal.ONE));
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.TEN);
        assertThat(condition).rejects(w);
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(11));
        assertThat(condition).accepts(w);
    }

    @Test
    void moreThan() {
        final MarketplaceFilterCondition condition = RelativeDiscountCondition.moreThan(Ratio.fromPercentage(10));
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getDiscount()).thenReturn(Optional.of(BigDecimal.ONE));
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.TEN);
        assertThat(condition).rejects(w);
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(9));
        assertThat(condition).accepts(w);
    }

    @Test
    void exact() {
        final MarketplaceFilterCondition condition = RelativeDiscountCondition.exact(Ratio.fromPercentage(8),
                Ratio.fromPercentage(10));
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getDiscount()).thenReturn(Optional.of(BigDecimal.ONE));
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.TEN);
        assertThat(condition).accepts(w);
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(9));
        assertThat(condition).rejects(w);
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(7));
        assertThat(condition).rejects(w);
    }

    @Test
    void hasDescription() {
        final MarketplaceFilterCondition condition = RelativeDiscountCondition.exact(Ratio.ONE, Ratio.ONE);
        assertThat(condition.getDescription()).isNotEmpty();
    }
}
