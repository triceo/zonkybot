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

package com.github.robozonky.api.notifications;

import java.time.ZonedDateTime;

/**
 * Mandatory parent for any event that may be fired any time during RoboZonky's runtime.
 * <p>
 * Subclasses must make sure that their class name ends with "Event", or else the default constructor of this class
 * will throw an exception.
 */
public interface Event {

    /**
     *
     * @return When the event instance was created.
     */
    ZonedDateTime getCreatedOn();

    /**
     *
     * @return When the event instance was requested to be created. Unless the event was instantiated lazily, will be
     *         the same as {@link #getCreatedOn()}.
     */
    default ZonedDateTime getConceivedOn() {
        return getCreatedOn();
    }

}
