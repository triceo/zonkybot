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

import static com.github.robozonky.strategy.natural.Audit.LOGGER;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class Util {

    private Util() {
        // no instances
    }

    static boolean isAcceptable(final ParsedStrategy strategy, final PortfolioOverview portfolio) {
        final Money invested = portfolio.getInvested();
        final Money investmentCeiling = strategy.getMaximumInvestmentSize();
        if (invested.compareTo(investmentCeiling) >= 0) {
            LOGGER.debug("Not recommending any loans due to reaching the ceiling.");
            return false;
        }
        return true;
    }
}
