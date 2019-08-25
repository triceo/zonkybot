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

import java.util.Currency;
import javax.xml.bind.annotation.XmlElement;

@Deprecated(forRemoval = true)
public class BankAccount extends BaseEntity {

    private long id;
    private long accountNo;
    private Currency currency;
    private int accountBank;
    private String accountName;
    private boolean enteredManually;

    BankAccount() {
        // for JAXB
    }

    @XmlElement
    public boolean isEnteredManually() {
        return enteredManually;
    }

    @XmlElement
    public int getAccountBank() {
        return accountBank;
    }

    @XmlElement
    public String getAccountName() {
        return accountName;
    }

    @XmlElement
    public long getAccountNo() {
        return accountNo;
    }

    @XmlElement
    public Currency getCurrency() {
        return currency;
    }

    @XmlElement
    public long getId() {
        return id;
    }
}
