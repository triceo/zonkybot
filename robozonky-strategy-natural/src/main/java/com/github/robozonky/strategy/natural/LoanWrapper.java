/*
 * Copyright 2018 The RoboZonky Project
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

import java.math.BigDecimal;
import java.util.Objects;

import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;

public class LoanWrapper implements Wrapper {

    private final Loan loan;
    private final String identifier;

    public LoanWrapper(final Loan loan) {
        this.loan = loan;
        this.identifier = "Loan #" + loan.getId();
    }

    public boolean isInsuranceActive() {
        return loan.isInsuranceActive();
    }

    public int getLoanId() {
        return loan.getId();
    }

    public Region getRegion() {
        return loan.getRegion();
    }

    public String getStory() {
        return loan.getStory();
    }

    public MainIncomeType getMainIncomeType() {
        return loan.getMainIncomeType();
    }

    public BigDecimal getInterestRate() {
        return loan.getInterestRate();
    }

    public Purpose getPurpose() {
        return loan.getPurpose();
    }

    @Override
    public Rating getRating() {
        return loan.getRating();
    }

    @Override
    public int getOriginalTermInMonths() {
        return loan.getTermInMonths();
    }

    public int getOriginalAmount() {
        return loan.getAmount();
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final LoanWrapper wrapper = (LoanWrapper) o;
        return Objects.equals(identifier, wrapper.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
}
