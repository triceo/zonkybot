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

package com.github.robozonky.app.daemon;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.remote.entities.LastPublishedItemImpl;
import com.github.robozonky.internal.remote.entities.ParticipationImpl;
import com.github.robozonky.internal.remote.entities.RestrictionsImpl;
import com.github.robozonky.test.mock.MockLoanBuilder;

class PurchasingOperationDescriptionTest extends AbstractZonkyLeveragingTest {

    @Test
    void getters() {
        final PurchasingOperationDescriptor d = new PurchasingOperationDescriptor();
        final Participation p = mock(ParticipationImpl.class);
        when(p.getId()).thenReturn(1l);
        final Loan l = MockLoanBuilder.fresh();
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, () -> l);
        assertThat(d.identify(pd)).isEqualTo(1);
        assertThat(d.getMinimumBalance(null)).isEqualTo(Money.from(1));
    }

    @Test
    void freshAccessorEveryTimeButTheyShareState() {
        final Zonky z = harmlessZonky();
        when(z.getLastPublishedParticipationInfo()).thenReturn(mock(LastPublishedItemImpl.class));
        final PowerTenant t = mockTenant(z);
        final PurchasingOperationDescriptor d = new PurchasingOperationDescriptor();
        final AbstractMarketplaceAccessor<ParticipationDescriptor> a1 = d.newMarketplaceAccessor(t);
        assertThat(a1.hasUpdates()).isTrue();
        final AbstractMarketplaceAccessor<ParticipationDescriptor> a2 = d.newMarketplaceAccessor(t);
        assertThat(a2.hasUpdates()).isFalse();
    }

    @Test
    void enabled() {
        final Zonky z = harmlessZonky();
        final PowerTenant t = mockTenant(z);
        when(z.getRestrictions()).thenReturn(new RestrictionsImpl(false));
        final PurchasingOperationDescriptor d = new PurchasingOperationDescriptor();
        assertThat(d.isEnabled(t)).isFalse();
    }

}
