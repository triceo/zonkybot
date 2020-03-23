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

package com.github.robozonky.api.remote.entities;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Supplier;

import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.internal.util.functional.Memoizer;

public class Statistics extends BaseEntity {

    private static final Supplier<Statistics> EMPTY = Memoizer.memoize(Statistics::emptyAndFresh);

    @XmlElement
    private Ratio profitability;
    private List<RiskPortfolio> riskPortfolio;
    private OffsetDateTime timestamp;

    /**
     * Data structures intentionally not implemented. We do not need this information.
     */
    @XmlElement
    private Object superInvestorOverview;
    @XmlElement
    private Object currentOverview;
    @XmlElement
    private Object overallOverview;

    private Statistics() {
        // for JAXB
    }

    public static Statistics empty() {
        return EMPTY.get();
    }

    public static Statistics emptyAndFresh() {
        final Statistics s = new Statistics();
        s.profitability = Ratio.ZERO;
        s.riskPortfolio = Collections.emptyList();
        s.timestamp = DateUtil.offsetNow();
        return s;
    }

    private static <T> List<T> unmodifiableOrEmpty(final List<T> possiblyNull) {
        return possiblyNull == null ? Collections.emptyList() : Collections.unmodifiableList(possiblyNull);
    }

    /**
     *
     * @return Empty if the user is relatively new on Zonky and they don't calculate this for them yet.
     */
    public Optional<Ratio> getProfitability() {
        return Optional.ofNullable(profitability);
    }

    @XmlElement
    public List<RiskPortfolio> getRiskPortfolio() { // "riskPortfolio" is null for new Zonky users
        return unmodifiableOrEmpty(riskPortfolio);
    }

    /**
     * Zonky only recalculates the information within this class every N hours.
     * 
     * @return This method returns the timestamp of the last recalculation.
     */
    @XmlElement
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Statistics.class.getSimpleName() + "[", "]")
            .add("profitability=" + profitability)
            .add("riskPortfolio=" + riskPortfolio)
            .add("timestamp=" + timestamp)
            .toString();
    }
}
