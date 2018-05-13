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

package com.github.robozonky.strategy.natural.conditions;

import com.github.robozonky.strategy.natural.InvestmentWrapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class RelativeLoanTermConditionTest {

    @Test
    void leftBoundWrong() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> new RelativeLoanTermCondition(-1, 0)).isInstanceOf(
                    IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new RelativeLoanTermCondition(0, -1)).isInstanceOf(
                    IllegalArgumentException.class);
        });
    }

    @Test
    void rightBoundWrong() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> new RelativeLoanTermCondition(101, 0)).isInstanceOf(
                    IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new RelativeLoanTermCondition(0, 101)).isInstanceOf(
                    IllegalArgumentException.class);
        });
    }

    @Test
    void boundaryCorrect() {
        final InvestmentWrapper l = mock(InvestmentWrapper.class);
        when(l.getOriginalTermInMonths()).thenReturn(2);
        when(l.getRemainingTermInMonths()).thenReturn(1);
        final RelativeLoanTermCondition condition = new RelativeLoanTermCondition(0, 100);
        assertThat(condition.test(l)).isTrue();
    }

    @Test
    void leftOutOfBounds() {
        final InvestmentWrapper l = mock(InvestmentWrapper.class);
        when(l.getOriginalTermInMonths()).thenReturn(2);
        when(l.getRemainingTermInMonths()).thenReturn(0);
        final RelativeLoanTermCondition condition = new RelativeLoanTermCondition(1, 100);
        assertThat(condition.test(l)).isFalse();
    }

    @Test
    void rightOutOfBounds() {
        final InvestmentWrapper l = mock(InvestmentWrapper.class);
        when(l.getOriginalTermInMonths()).thenReturn(2);
        when(l.getRemainingTermInMonths()).thenReturn(1);
        final RelativeLoanTermCondition condition = new RelativeLoanTermCondition(0, 20);
        assertThat(condition.test(l)).isFalse();
    }
}
