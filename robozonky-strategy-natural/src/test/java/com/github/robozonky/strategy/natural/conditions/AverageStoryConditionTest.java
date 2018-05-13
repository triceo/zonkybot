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

package com.github.robozonky.strategy.natural.conditions;

import com.github.robozonky.strategy.natural.LoanWrapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AverageStoryConditionTest {

    @Test
    void longerNotOk() {
        final LoanWrapper l = mock(LoanWrapper.class);
        final String story = StringUtils.leftPad("", AbstractStoryCondition.LONG_STORY_THRESHOLD + 1, '*');
        when(l.getStory()).thenReturn(story);
        assertThat(new AverageStoryCondition().test(l)).isFalse();
    }

    @Test
    void leftBoundOk() {
        final LoanWrapper l = mock(LoanWrapper.class);
        final String story = StringUtils.leftPad("", AbstractStoryCondition.SHORT_STORY_THRESHOLD + 1, '*');
        when(l.getStory()).thenReturn(story);
        assertThat(new AverageStoryCondition().test(l)).isTrue();
    }

    @Test
    void rightBoundOk() {
        final LoanWrapper l = mock(LoanWrapper.class);
        final String story = StringUtils.leftPad("", AbstractStoryCondition.LONG_STORY_THRESHOLD, '*');
        when(l.getStory()).thenReturn(story);
        assertThat(new AverageStoryCondition().test(l)).isTrue();
    }

    @Test
    void shorterNotOk() {
        final LoanWrapper l = mock(LoanWrapper.class);
        final String story = StringUtils.leftPad("", AbstractStoryCondition.SHORT_STORY_THRESHOLD - 1, '*');
        when(l.getStory()).thenReturn(story);
        assertThat(new AverageStoryCondition().test(l)).isFalse();
    }
}
