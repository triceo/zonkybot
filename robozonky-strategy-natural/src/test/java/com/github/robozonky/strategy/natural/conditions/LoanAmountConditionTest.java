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

import com.github.robozonky.strategy.natural.wrappers.Wrapper;

class LoanAmountConditionTest {

    @Test
    void lessThan() {
        final MarketplaceFilterCondition condition = LoanAmountCondition.lessThan(1);
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getOriginalAmount()).thenReturn(0);
        assertThat(condition).accepts(w);
        when(w.getOriginalAmount()).thenReturn(1);
        assertThat(condition).rejects(w);
    }

    @Test
    void moreThan() {
        final MarketplaceFilterCondition condition = LoanAmountCondition.moreThan(0);
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getOriginalAmount()).thenReturn(0);
        assertThat(condition).rejects(w);
        when(w.getOriginalAmount()).thenReturn(1);
        assertThat(condition).accepts(w);
    }

    @Test
    void exact() {
        final MarketplaceFilterCondition condition = LoanAmountCondition.exact(0, 1);
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getOriginalAmount()).thenReturn(0);
        assertThat(condition).accepts(w);
        when(w.getOriginalAmount()).thenReturn(1);
        assertThat(condition).accepts(w);
        when(w.getOriginalAmount()).thenReturn(2);
        assertThat(condition).rejects(w);
    }
}
