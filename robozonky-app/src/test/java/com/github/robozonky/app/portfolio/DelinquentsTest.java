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

package com.github.robozonky.app.portfolio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;

class DelinquentsTest extends AbstractZonkyLeveragingTest {

    private static final Function<Loan, Investment> INVESTMENT_SUPPLIER =
            (id) -> Investment.custom().build();
    private static final BiFunction<Loan, LocalDate, Collection<Development>> COLLECTIONS_SUPPLIER =
            (l, s) -> Collections.emptyList();
    private final static Random RANDOM = new Random(0);

    @Test
    void empty() {
        assertThat(Delinquents.getDelinquents()).isEmpty();
        assertThat(this.getNewEvents()).isEmpty();
    }

    @Test
    void newDelinquence() {
        final Loan l = Loan.custom()
                .setId(RANDOM.nextInt(10000))
                .setRating(Rating.D)
                .setAmount(200)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Investment i = Investment.fresh(l, 200)
                .setNextPaymentDate(OffsetDateTime.now().minusDays(1))
                .build();
        final Function<Integer, Loan> f = (id) -> l;
        final Function<Loan, Investment> lif = (loan) -> i;
        // make sure new delinquencies are reported and stored
        Delinquents.update(Collections.singleton(i), lif, f, COLLECTIONS_SUPPLIER);
        assertSoftly(softly -> {
            softly.assertThat(Delinquents.getDelinquents()).hasSize(1);
            softly.assertThat(this.getNewEvents()).hasSize(1);
        });
        assertThat(this.getNewEvents().get(0)).isInstanceOf(LoanNowDelinquentEvent.class);
        // make sure delinquencies are persisted even when there are none present
        Delinquents.update(Collections.emptyList(), lif, f, COLLECTIONS_SUPPLIER);
        assertSoftly(softly -> {
            softly.assertThat(Delinquents.getDelinquents()).hasSize(1);
            softly.assertThat(this.getNewEvents()).hasSize(2);
        });
        assertThat(this.getNewEvents().get(1)).isInstanceOf(LoanNoLongerDelinquentEvent.class);
        // and when they are no longer active, they're gone for good
        Delinquents.update(Collections.emptyList(), lif, f, COLLECTIONS_SUPPLIER);
        assertThat(Delinquents.getDelinquents()).hasSize(0);
    }

    @Test
    void oldDelinquency() {
        final Loan l = Loan.custom()
                .setId(RANDOM.nextInt(10000))
                .setRating(Rating.D)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Investment i = Investment.fresh(l, 200)
                .setNextPaymentDate(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID))
                .build();
        final Function<Integer, Loan> f = (id) -> l;
        final Function<Loan, Investment> lif = (loan) -> i;
        // make sure new delinquencies are reported and stored
        Delinquents.update(Collections.singleton(i), lif, f, COLLECTIONS_SUPPLIER);
        assertSoftly(softly -> {
            softly.assertThat(Delinquents.getDelinquents()).hasSize(1);
            softly.assertThat(this.getNewEvents()).hasSize(5); // all 5 delinquency events
        });
    }

    @Test
    void noLongerDelinquent() {
        final Loan l = Loan.custom()
                .setId(RANDOM.nextInt(10000))
                .setRating(Rating.D)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Investment i = Investment.fresh(l, 200)
                .setNextPaymentDate(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID))
                .build();
        final Function<Integer, Loan> f = (id) -> l;
        final Function<Loan, Investment> lif = (loan) -> i;
        // register delinquence
        Delinquents.update(Collections.singleton(i), lif, f, COLLECTIONS_SUPPLIER);
        this.readPreexistingEvents(); // ignore events just emitted
        // the investment is no longer delinquent
        Delinquents.update(Collections.emptyList(), lif, f, COLLECTIONS_SUPPLIER);
        assertThat(this.getNewEvents()).hasSize(1).first().isInstanceOf(LoanNoLongerDelinquentEvent.class);
    }

    @Test
    void defaulted() {
        final Loan l = Loan.custom()
                .setId(RANDOM.nextInt(10000))
                .setRating(Rating.D)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Investment i = Investment.fresh(l, 200)
                .setPaymentStatus(PaymentStatus.PAID_OFF)
                .setNextPaymentDate(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID))
                .build();
        final Function<Integer, Loan> f = (id) -> l;
        final Function<Loan, Investment> lif = (loan) -> i;
        // register delinquency
        Delinquents.update(Collections.singleton(i), lif, f, COLLECTIONS_SUPPLIER);
        this.readPreexistingEvents(); // ignore events just emitted
        // the investment is defaulted
        Delinquents.update(Collections.emptyList(), lif, f, COLLECTIONS_SUPPLIER);
        assertThat(this.getNewEvents()).hasSize(1).first().isInstanceOf(LoanDefaultedEvent.class);
    }

    @Test
    void paid() {
        final Loan l = Loan.custom()
                .setId(RANDOM.nextInt(10000))
                .setRating(Rating.D)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Investment i = Investment.fresh(l, 200)
                .setPaymentStatus(PaymentStatus.PAID)
                .setNextPaymentDate(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID))
                .build();
        final Function<Integer, Loan> f = (id) -> l;
        final Function<Loan, Investment> lif = (loan) -> i;
        // register delinquence
        Delinquents.update(Collections.singleton(i), lif, f, COLLECTIONS_SUPPLIER);
        this.readPreexistingEvents(); // ignore events just emitted
        // the investment is paid
        Delinquents.update(Collections.emptyList(), lif, f, COLLECTIONS_SUPPLIER);
        assertThat(this.getNewEvents()).isEmpty();
    }

    @Test
    void defaultUpdateTime() {
        assertThat(Delinquents.getLastUpdateTimestamp()).isBefore(OffsetDateTime.now());
    }
}
