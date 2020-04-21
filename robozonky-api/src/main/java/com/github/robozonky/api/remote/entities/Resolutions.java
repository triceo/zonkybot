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

package com.github.robozonky.api.remote.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import javax.xml.bind.annotation.XmlElement;

public class Resolutions {

    private List<ResolutionRequest> resolutions = Collections.emptyList();

    public Resolutions(final Collection<ResolutionRequest> resolutions) {
        this.resolutions = new ArrayList<>(resolutions);
    }

    Resolutions() {
        // for JAXB
    }

    @XmlElement
    public List<ResolutionRequest> getResolutions() {
        return resolutions;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Resolutions.class.getSimpleName() + "[", "]")
            .add("resolutions=" + resolutions)
            .toString();
    }
}
