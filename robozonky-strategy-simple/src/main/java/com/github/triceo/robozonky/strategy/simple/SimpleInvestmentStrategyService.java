/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.strategy.simple;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.remote.Rating;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import com.github.triceo.robozonky.strategy.InvestmentStrategyParseException;
import com.github.triceo.robozonky.strategy.InvestmentStrategyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleInvestmentStrategyService implements InvestmentStrategyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleInvestmentStrategyService.class);
    // below are strings that identify different variables in the strategy file

    private static <T> Optional<T> getValue(final ImmutableConfiguration config, final String property,
                                            final Function<String, T> supplier) {
        return config.containsKey(property) ? Optional.of(supplier.apply(property)) : Optional.empty();
    }

    private static <T> T getValue(final ImmutableConfiguration config, final Rating r, final String property,
                                  final Function<String, T> supplier) {
        final String propertyName = Util.join(property, r.name());
        return SimpleInvestmentStrategyService.getValue(config, propertyName, supplier).orElseGet(() -> {
            final String fallbackPropertyName = Util.join(property, "default");
            return SimpleInvestmentStrategyService.getValue(config, fallbackPropertyName, supplier)
                    .orElseThrow(() -> new IllegalStateException("Investment strategy is incomplete. " +
                            "Missing value for '" + property + "' and rating '" + r + '\''));
        });
    }

    private static ImmutableConfiguration getConfig(final File strategyFile) throws InvestmentStrategyParseException {
        return ImmutableConfiguration.from(strategyFile);
    }

    static int getMinimumBalance(final ImmutableConfiguration config) {
        final int minimumBalance = SimpleInvestmentStrategyService.getValue(config,
                StrategyFileProperties.MINIMUM_BALANCE, config::getInt)
                .orElseThrow(() -> new IllegalStateException("Minimum balance is missing."));
        if (minimumBalance < InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED) {
            throw new IllegalStateException("Minimum balance is less than "
                    + InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED + " CZK.");
        }
        return minimumBalance;
    }

    static int getMaximumInvestment(final ImmutableConfiguration config) {
        final int maximumInvestment = SimpleInvestmentStrategyService.getValue(config,
                StrategyFileProperties.MAXIMUM_INVESTMENT, config::getInt)
                .orElseThrow(() -> new IllegalStateException("Maximum investment is missing."));
        if (maximumInvestment < InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED) {
            throw new IllegalStateException("Maximum investment is less than "
                    + InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED + " CZK.");
        }
        return maximumInvestment;
    }

    private static StrategyPerRating createIndividualStrategy(final Rating r, final BigDecimal targetShare,
                                                              final int minTerm, final int maxTerm,
                                                              final int minAskAmount, final int maxAskAmount,
                                                              final int minLoanAmount, final int maxLoanAmount,
                                                              final BigDecimal minLoanShare,
                                                              final BigDecimal maxLoanShare,
                                                              final boolean preferLongerTerms) {
        SimpleInvestmentStrategyService.LOGGER.debug("Adding strategy for rating '{}'.", r.getCode());
        SimpleInvestmentStrategyService.LOGGER.debug("Target share for rating '{}' among total investments is {}.",
                r.getCode(), targetShare);
        SimpleInvestmentStrategyService.LOGGER.debug("Range of acceptable loan amounts for rating '{}' is "
                + "<{}, {}> CZK.", r.getCode(), minAskAmount, maxAskAmount < 0 ? "+inf" : maxAskAmount);
        SimpleInvestmentStrategyService.LOGGER.debug("Acceptable range of investment terms for rating '{}' is "
                + "<{}, {}) months.", r.getCode(), minTerm == -1 ? 0 : minTerm, maxTerm < 0 ? "+inf" : maxTerm + 1);
        SimpleInvestmentStrategyService.LOGGER.debug("Acceptable range of investment amount for rating '{}' is "
                + "<{}, {}>.", r.getCode(), minLoanAmount, maxLoanAmount < 0 ? "+inf" : maxLoanAmount);
        SimpleInvestmentStrategyService.LOGGER.debug("Acceptable range of investment share for rating '{}' is "
                + "<{}, {}>.", r.getCode(), minLoanShare, maxLoanShare);
        if (preferLongerTerms) {
            SimpleInvestmentStrategyService.LOGGER.debug("Rating '{}' will prefer longer terms.", r.getCode());
        } else {
            SimpleInvestmentStrategyService.LOGGER.debug("Rating '{}' will prefer shorter terms.", r.getCode());
        }
        return new StrategyPerRating(r, targetShare, minTerm, maxTerm, minLoanAmount, maxLoanAmount, minLoanShare,
                maxLoanShare, minAskAmount, maxAskAmount, preferLongerTerms);
    }

    private static StrategyPerRating parseRating(final Rating rating, final ImmutableConfiguration config) {
        final boolean preferLongerTerms = SimpleInvestmentStrategyService.getValue(config, rating,
                StrategyFileProperties.PREFER_LONGER_TERMS, config::getBoolean);
        final BigDecimal targetShare = SimpleInvestmentStrategyService.getValue(config, rating,
                StrategyFileProperties.TARGET_SHARE, config::getBigDecimal);
        if (!Util.isBetweenZeroAndOne(targetShare)) {
            throw new IllegalStateException("Target share for rating " + rating + " outside of range <0, 1>: "
                    + targetShare);
        }
        // terms
        final int minTerm = SimpleInvestmentStrategyService.getValue(config, rating,
                StrategyFileProperties.MINIMUM_TERM, config::getInt);
        if (minTerm < -1) {
            throw new IllegalStateException("Minimum acceptable term for rating " + rating + " negative.");
        }
        final int maxTerm = SimpleInvestmentStrategyService.getValue(config, rating,
                StrategyFileProperties.MAXIMUM_TERM, config::getInt);
        if (maxTerm < -1) {
            throw new IllegalStateException("Maximum acceptable term for rating " + rating + " negative.");
        } else if (minTerm > maxTerm && maxTerm != -1) {
            throw new IllegalStateException("Maximum acceptable term for rating " + rating + " less than minimum.");
        }
        // loan asks
        final int minAskAmount = SimpleInvestmentStrategyService.getValue(config, rating,
                StrategyFileProperties.MINIMUM_ASK, config::getInt);
        if (minAskAmount < -1) {
            throw new IllegalStateException("Minimum acceptable ask amount for rating " + rating + " negative.");
        }
        final int maxAskAmount = SimpleInvestmentStrategyService.getValue(config, rating,
                StrategyFileProperties.MAXIMUM_ASK, config::getInt);
        if (maxAskAmount < -1) {
            throw new IllegalStateException("Maximum acceptable ask for rating " + rating + " negative.");
        } else if (minAskAmount > maxAskAmount && maxAskAmount != -1) {
            throw new IllegalStateException("Maximum acceptable ask for rating " + rating + " less than minimum.");
        }
        // investment amounts
        final int minLoanAmount = SimpleInvestmentStrategyService.getValue(config, rating,
                StrategyFileProperties.MINIMUM_LOAN_AMOUNT, config::getInt);
        if (minLoanAmount < InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED) {
            throw new IllegalStateException("Minimum investment amount for rating " + rating
                    + " less than minimum investment allowed.");
        }
        final int maxLoanAmount = SimpleInvestmentStrategyService.getValue(config, rating,
                StrategyFileProperties.MAXIMUM_LOAN_AMOUNT, config::getInt);
        if (maxLoanAmount != -1 && maxLoanAmount < minLoanAmount) {
            throw new IllegalStateException("Maximum investment amount for rating " + rating + " less than minimum.");
        }
        final BigDecimal minLoanShare = SimpleInvestmentStrategyService.getValue(config, rating,
                StrategyFileProperties.MINIMUM_LOAN_SHARE, config::getBigDecimal);
        if (!Util.isBetweenZeroAndOne(minLoanShare)) {
            throw new IllegalStateException("Minimum investment share for rating " + rating
                    + " outside of range <0, 1>: " + targetShare);
        }
        final BigDecimal maxLoanShare = SimpleInvestmentStrategyService.getValue(config, rating,
                StrategyFileProperties.MAXIMUM_LOAN_SHARE, config::getBigDecimal);
        if (!Util.isBetweenAAndB(maxLoanShare, minLoanShare, BigDecimal.ONE)) {
            throw new IllegalStateException("Maximum investment share for rating " + rating
                    + " outside of range (min, 1>: " + targetShare);
        }
        return SimpleInvestmentStrategyService.createIndividualStrategy(rating, targetShare, minTerm, maxTerm,
                minAskAmount, maxAskAmount, minLoanAmount, maxLoanAmount, minLoanShare, maxLoanShare,
                preferLongerTerms);
    }

    @Override
    public InvestmentStrategy parse(final File strategyFile) throws InvestmentStrategyParseException {
        try {
            SimpleInvestmentStrategyService.LOGGER.info("Using strategy: '{}'", strategyFile.getAbsolutePath());
            final ImmutableConfiguration c = SimpleInvestmentStrategyService.getConfig(strategyFile);
            final int minimumBalance = SimpleInvestmentStrategyService.getMinimumBalance(c);
            SimpleInvestmentStrategyService.LOGGER.debug("Minimum balance to invest must be {} CZK.", minimumBalance);
            final int maximumInvestment = SimpleInvestmentStrategyService.getMaximumInvestment(c);
            SimpleInvestmentStrategyService.LOGGER.debug("Maximum investment must not exceed {} CZK.", maximumInvestment);
            final Map<Rating, StrategyPerRating> individualStrategies = Arrays.stream(Rating.values())
                    .collect(Collectors.toMap(Function.identity(), r -> SimpleInvestmentStrategyService.parseRating(r, c)));
            return new SimpleInvestmentStrategy(minimumBalance, maximumInvestment, individualStrategies);
        } catch (final IllegalStateException ex) {
            throw new InvestmentStrategyParseException(ex);
        }
    }

    @Override
    public boolean isSupported(final File strategyFile) {
        return strategyFile.getAbsolutePath().endsWith(".cfg");
    }

}
