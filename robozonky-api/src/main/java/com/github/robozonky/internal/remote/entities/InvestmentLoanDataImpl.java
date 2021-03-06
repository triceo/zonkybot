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

package com.github.robozonky.internal.remote.entities;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import javax.json.bind.annotation.JsonbProperty;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Borrower;
import com.github.robozonky.api.remote.entities.Instalments;
import com.github.robozonky.api.remote.entities.InvestmentLoanData;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.LoanHealthStats;
import com.github.robozonky.api.remote.enums.DetailLabel;
import com.github.robozonky.api.remote.enums.Label;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;

public class InvestmentLoanDataImpl implements InvestmentLoanData {

    private int id;
    private int activeLoanOrdinal;
    private int dpd;
    private boolean hasCollectionHistory;
    private String title;
    private String story;
    private Money annuity;
    @JsonbProperty(nillable = true)
    private Label label;
    @JsonbProperty(nillable = true)
    private Set<DetailLabel> detailLabels;
    private BorrowerImpl borrower;
    private LoanHealthStatsImpl healthStats;
    private Purpose purpose;
    private InstalmentsImpl payments;
    private Ratio revenueRate;
    private Ratio interestRate;

    public InvestmentLoanDataImpl() {
        // For JSON-B.
    }

    public InvestmentLoanDataImpl(Loan loan) {
        this(loan, null);
    }

    public InvestmentLoanDataImpl(Loan loan, LoanHealthStats loanHealthStats) {
        this.id = loan.getId();
        this.dpd = loanHealthStats == null ? 0 : loanHealthStats.getCurrentDaysDue();
        this.hasCollectionHistory = dpd > 0;
        this.title = loan.getName();
        this.story = loan.getStory();
        this.annuity = loan.getAnnuity();
        this.borrower = new BorrowerImpl(loan.getMainIncomeType(), loan.getRegion());
        this.healthStats = (LoanHealthStatsImpl) loanHealthStats;
        this.purpose = loan.getPurpose();
        this.payments = new InstalmentsImpl(loan.getTermInMonths());
        this.interestRate = loan.getInterestRate();
        this.revenueRate = loan.getRevenueRate()
            .orElseGet(() -> Rating.forInterestRate(interestRate)
                .getMaximalRevenueRate());
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    @Override
    public int getActiveLoanOrdinal() {
        return activeLoanOrdinal;
    }

    public void setActiveLoanOrdinal(final int activeLoanOrdinal) {
        this.activeLoanOrdinal = activeLoanOrdinal;
    }

    @Override
    public int getDpd() {
        return dpd;
    }

    public void setDpd(final int dpd) {
        this.dpd = dpd;
    }

    @Override
    public boolean hasCollectionHistory() {
        return hasCollectionHistory;
    }

    public void setHasCollectionHistory(final boolean hasCollectionHistory) {
        this.hasCollectionHistory = hasCollectionHistory;
    }

    @Override
    public String getTitle() {
        return requireNonNull(title);
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    @Override
    public Optional<String> getStory() {
        return Optional.ofNullable(story);
    }

    public void setStory(final String story) {
        this.story = story;
    }

    @Override
    public Optional<Money> getAnnuity() {
        return Optional.ofNullable(annuity);
    }

    public void setAnnuity(final Money annuity) {
        this.annuity = annuity;
    }

    @Override
    public Optional<Label> getLabel() {
        return Optional.ofNullable(label);
    }

    public void setLabel(final Label label) {
        this.label = label;
    }

    @Override
    public Set<DetailLabel> getDetailLabels() {
        return Optional.ofNullable(detailLabels)
            .map(Collections::unmodifiableSet)
            .orElse(Collections.emptySet());
    }

    public void setDetailLabels(final Set<DetailLabel> detailLabels) {
        this.detailLabels = EnumSet.copyOf(detailLabels);
    }

    @Override
    public Borrower getBorrower() {
        return requireNonNull(borrower);
    }

    public void setBorrower(final BorrowerImpl borrower) {
        this.borrower = borrower;
    }

    @Override
    public Optional<LoanHealthStats> getHealthStats() {
        return Optional.ofNullable(healthStats);
    }

    public void setHealthStats(final LoanHealthStatsImpl healthStats) {
        this.healthStats = healthStats;
    }

    @Override
    public Purpose getPurpose() {
        return requireNonNull(purpose);
    }

    public void setPurpose(final Purpose purpose) {
        this.purpose = purpose;
    }

    @Override
    public Instalments getPayments() {
        return requireNonNull(payments);
    }

    public void setPayments(final InstalmentsImpl payments) {
        this.payments = payments;
    }

    @Override
    public Ratio getRevenueRate() {
        return requireNonNull(revenueRate);
    }

    public void setRevenueRate(final Ratio revenueRate) {
        this.revenueRate = revenueRate;
    }

    @Override
    public Ratio getInterestRate() {
        return requireNonNull(interestRate);
    }

    public void setInterestRate(final Ratio interestRate) {
        this.interestRate = interestRate;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InvestmentLoanDataImpl.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("annuity=" + annuity)
            .add("interestRate=" + interestRate)
            .add("revenueRate=" + revenueRate)
            .add("purpose=" + purpose)
            .add("payments=" + payments)
            .add("healthStats=" + healthStats)
            .add("borrower=" + borrower)
            .add("dpd=" + dpd)
            .add("hasCollectionHistory=" + hasCollectionHistory)
            .add("title='" + title + "'")
            .add("label=" + label)
            .add("detailLabels=" + detailLabels)
            .add("activeLoanOrdinal=" + activeLoanOrdinal)
            .toString();
    }
}
