/*
 * Copyright 2020 The RoboZonky Project
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.github.robozonky.strategy.natural.wrappers.Wrapper;

class ShortStoryConditionTest {

    @Test
    void longerNotOk() {
        final Wrapper<?> l = mock(Wrapper.class);
        final String story = StringUtils.leftPad("", AbstractStoryCondition.SHORT_STORY_THRESHOLD + 1, '*');
        when(l.getStory()).thenReturn(story);
        assertThat(new ShortStoryCondition().test(l)).isFalse();
    }

    @Test
    void shorterOk() {
        final Wrapper<?> l = mock(Wrapper.class);
        final String story = StringUtils.leftPad("", AbstractStoryCondition.SHORT_STORY_THRESHOLD, '*');
        when(l.getStory()).thenReturn(story);
        assertThat(new ShortStoryCondition().test(l)).isTrue();
    }

    @Test
    void expectsRemoteRequest() {
        final AbstractStoryCondition condition = new ShortStoryCondition();
        assertThat(condition.mayRequireRemoteRequests()).isTrue();
    }

}
