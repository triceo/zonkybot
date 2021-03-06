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

package com.github.robozonky.strategy.natural;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.test.AbstractMinimalRoboZonkyTest;

class DefaultValuesTest extends AbstractMinimalRoboZonkyTest {

    @Test
    void construct() {
        final DefaultPortfolio p = DefaultPortfolio.BALANCED;
        final DefaultValues sut = new DefaultValues(p);
        assertSoftly(softly -> {
            softly.assertThat(sut.getPortfolio())
                .isSameAs(p);
            softly.assertThat(sut.getInvestmentSize()
                .getMinimumInvestment())
                .isEqualTo(Money.ZERO);
        });
    }

    @Test
    void setTargetPortfolioSize() {
        final DefaultPortfolio p = DefaultPortfolio.PROGRESSIVE;
        final DefaultValues sut = new DefaultValues(p);
        assertThat(sut.getTargetPortfolioSize()).isEqualTo(Money.from(Long.MAX_VALUE));
        sut.setTargetPortfolioSize(400);
        assertThat(sut.getTargetPortfolioSize()).isEqualTo(Money.from(400));
    }

    @Test
    void setWrongTargetPortfolioSize() {
        final DefaultPortfolio p = DefaultPortfolio.EMPTY;
        final DefaultValues sut = new DefaultValues(p);
        assertThatThrownBy(() -> sut.setTargetPortfolioSize(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setExitStrategy() {
        setClock(Clock.fixed(Instant.EPOCH, Defaults.ZONKYCZ_ZONE_ID));
        final ExitProperties p = new ExitProperties(DateUtil.zonedNow()
            .plusMonths(1)
            .toLocalDate());
        final DefaultValues v = new DefaultValues(DefaultPortfolio.EMPTY);
        assertThat(v.getMonthsBeforeExit()).isEqualTo(-1);
        v.setExitProperties(p);
        assertThat(v.getMonthsBeforeExit()).isEqualTo(1);
    }
}
