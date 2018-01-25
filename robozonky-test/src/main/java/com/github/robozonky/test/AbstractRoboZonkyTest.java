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

package com.github.robozonky.test;

import java.io.File;

import com.github.robozonky.internal.api.Settings;
import com.github.robozonky.test.schedulers.TestingSchedulerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a suggested parent class for all RoboZonky tests using this module. It will make sure to clear shared state
 * before and after each state, so that tests don't have unexpected and well-hidden dependencies.
 */
public abstract class AbstractRoboZonkyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRoboZonkyTest.class);

    @BeforeAll
    static void loadSystemProperties() {
        SystemProperties.INSTANCE.save();
    }

    @AfterEach
    void restoreSystemProperties() {
        SystemProperties.INSTANCE.restore();
    }

    @BeforeEach
    @AfterEach
    protected void reinitScheduler() {
        Mockito.reset(TestingSchedulerService.MOCK_SERVICE);
    }

    @BeforeEach
    @AfterEach
    protected void deleteState() {
        final File f = Settings.INSTANCE.getStateFile();
        AbstractRoboZonkyTest.LOGGER.info("Deleted {}: {}.", f.getAbsolutePath(), f.delete());
    }
}
