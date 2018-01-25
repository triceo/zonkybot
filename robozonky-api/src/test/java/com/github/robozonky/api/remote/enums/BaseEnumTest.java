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

package com.github.robozonky.api.remote.enums;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class BaseEnumTest {

    private static <T extends BaseEnum> void bidirectionality(final T i, final Function<String, T> converter) {
        final String code = i.getCode();
        final BaseEnum type = converter.apply(code);
        Assertions.assertThat(type).isSameAs(i);
    }

    private static <T extends BaseEnum> void wrong(final Function<String, T> converter) {
        Assertions.assertThatThrownBy(() -> converter.apply(UUID.randomUUID().toString()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static <T extends BaseEnum> Stream<DynamicTest> code(final Class<T> clz, final T[] instances,
                                                                 final Function<String, T> converter) {
        final Stream<DynamicTest> t = Stream.of(instances)
                .map(value -> dynamicTest("bidirectional " + value.getClass().getSimpleName() + '.' + value,
                                          () -> bidirectionality(value, converter)));
        final Stream<DynamicTest> t2 =
                Stream.of(dynamicTest("wrong " + clz.getSimpleName(), () -> wrong(Purpose::findByCode)));
        return Stream.concat(t, t2);
    }

    @TestFactory
    public Stream<DynamicTest> code() {
        final Stream<DynamicTest> purpose = code(Purpose.class, Purpose.values(), Purpose::findByCode);
        final Stream<DynamicTest> mainIncomeType = code(MainIncomeType.class, MainIncomeType.values(),
                                                        MainIncomeType::findByCode);
        final Stream<DynamicTest> rating = code(Rating.class, Rating.values(), Rating::findByCode);
        final Stream<DynamicTest> region = code(Region.class, Region.values(), Region::findByCode);
        return Stream.concat(Stream.concat(purpose, mainIncomeType), Stream.concat(rating, region));
    }
}
