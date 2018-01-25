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

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class LoanInterestRateConditionTest {

    @Test
    public void leftBoundary() {
        Assertions.assertThatThrownBy(() -> new LoanInterestRateCondition(BigDecimal.ZERO.subtract(BigDecimal.ONE)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void rightBoundary() {
        final BigDecimal maxInterestRate = LoanInterestRateCondition.moreThan(BigDecimal.valueOf(Double.MAX_VALUE));
        Assertions.assertThatThrownBy(() -> new LoanInterestRateCondition(BigDecimal.ZERO, maxInterestRate))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
