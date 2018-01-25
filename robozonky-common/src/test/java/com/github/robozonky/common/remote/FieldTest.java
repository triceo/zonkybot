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

package com.github.robozonky.common.remote;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class FieldTest {

    private <T> void unique(final Field<T>... values) {
        final Set<String> uniqueValues = Stream.of(values).map(Field::id).collect(Collectors.toSet());
        Assertions.assertThat(uniqueValues).hasSize(values.length);
    }

    @Test
    public void uniqueInvestmentFields() {
        unique(InvestmentField.values());
    }

    @Test
    public void uniqueLoanFields() {
        unique(LoanField.values());
    }
}
