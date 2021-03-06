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

package com.github.robozonky.app.summaries;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.SellStatus;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.remote.entities.InvestmentImpl;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.internal.util.functional.Tuple;
import com.github.robozonky.internal.util.functional.Tuple2;

final class Util {

    private static final Logger LOGGER = LogManager.getLogger(Util.class);
    private static final Collector<Money, ?, Money> ADDING_REDUCTION = reducing(Money.ZERO, Money::add);

    private Util() {
        // no instances
    }

    static Map<Ratio, Money> getAmountsAtRisk(final Tenant tenant) {
        return tenant.call(Zonky::getDelinquentInvestments)
            .parallel() // possibly many pages' worth of results; fetch in parallel
            .collect(groupingBy(investment -> investment.getLoan()
                .getInterestRate(), HashMap::new, mapping(i -> {
                    final Money remaining = i.getPrincipal()
                        .getUnpaid();
                    // TODO Convince Zonky to add penalties back to the API.
                    final Money principalNotYetReturned = remaining.subtract(i.getInterest()
                        .getPaid())
                        .max(remaining.getZero());
                    LOGGER.debug("Delinquent: {} in loan #{}, investment #{}.",
                            principalNotYetReturned, i.getLoan()
                                .getId(),
                            i.getId());
                    return principalNotYetReturned;
                }, ADDING_REDUCTION)));
    }

    /**
     * @param tenant
     * @return First is sellable with or without fee, second just without.
     */
    static Tuple2<Map<Ratio, Money>, Map<Ratio, Money>> getAmountsSellable(final Tenant tenant) {
        var allSellableInvestments = tenant.call(Zonky::getSellableInvestments)
            .parallel() // Possibly many pages of HTTP requests, plus possibly subsequent sellInfo HTTP requests.
            .map(investment -> {
                // Do everything we can to avoid retrieving the optional remote smpSellInfo.
                var rating = investment.getLoan()
                    .getInterestRate();
                var fee = investment.getSellStatus() == SellStatus.SELLABLE_WITHOUT_FEE ? Money.ZERO
                        : investment.getSmpSellInfo()
                            .map(si -> si.getFee()
                                .getValue())
                            .orElse(Money.ZERO);
                var sellPrice = InvestmentImpl.determineSellPrice(investment);
                return Tuple.of(rating, sellPrice, fee);
            })
            .filter(data -> !data._2.isZero()) // Filter out empty loans. Zonky shouldn't send those, but happened.
            .collect(Collectors.toList());
        var sellableWithoutFees = allSellableInvestments.stream()
            .filter(data -> data._3.isZero())
            .collect(groupingBy(t -> t._1, HashMap::new, mapping(t -> t._2, ADDING_REDUCTION)));
        var sellable = allSellableInvestments.stream()
            .map(t -> Tuple.of(t._1, t._2.subtract(t._3))) // Account for the sale fee.
            .collect(groupingBy(t -> t._1, HashMap::new, mapping(t -> t._2, ADDING_REDUCTION)));
        return Tuple.of(sellable, sellableWithoutFees);
    }
}
