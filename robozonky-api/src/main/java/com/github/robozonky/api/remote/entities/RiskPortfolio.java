/*
 * Copyright 2019 The RoboZonky Project
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

import com.github.robozonky.api.remote.enums.Rating;

import javax.xml.bind.annotation.XmlElement;
import java.math.BigDecimal;

public class RiskPortfolio extends BaseEntity {

    private BigDecimal unpaid;
    private BigDecimal paid;
    private BigDecimal due;
    private BigDecimal totalAmount;
    private Rating rating;

    RiskPortfolio() {
        // for JAXB
    }

    public RiskPortfolio(final Rating rating, final long paid, final long unpaid, final long due) {
        this.paid = BigDecimal.valueOf(paid);
        this.unpaid = BigDecimal.valueOf(unpaid);
        this.due = BigDecimal.valueOf(due);
        this.rating = rating;
        this.totalAmount = BigDecimal.valueOf(paid + unpaid + due);
    }

    @XmlElement
    public BigDecimal getUnpaid() {
        return unpaid;
    }

    @XmlElement
    public BigDecimal getPaid() {
        return paid;
    }

    @XmlElement
    public BigDecimal getDue() {
        return due;
    }

    @XmlElement
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    @XmlElement
    public Rating getRating() {
        return rating;
    }
}
