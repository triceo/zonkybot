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

package com.github.robozonky.app.configuration.daemon;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.api.marketplaces.Marketplace;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.ShutdownHook;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DaemonInvestmentModeTest extends AbstractZonkyLeveragingTest {

    private final Lifecycle lifecycle = new Lifecycle();

    @Test
    public void get() throws Exception {
        final Authenticated a = mockAuthentication(Mockito.mock(Zonky.class));
        final Investor.Builder b = new Investor.Builder().asDryRun();
        final Marketplace m = Mockito.mock(Marketplace.class);
        final ExecutorService e = Executors.newFixedThreadPool(1);
        final PortfolioUpdater p = Mockito.mock(PortfolioUpdater.class);
        final BlockedAmountsUpdater bau = Mockito.mock(BlockedAmountsUpdater.class);
        try (final DaemonInvestmentMode d = new DaemonInvestmentMode(a, p, b, m, Mockito.mock(StrategyProvider.class),
                                                                     bau, Duration.ofMinutes(1), Duration.ofSeconds(1),
                                                                     Duration.ofSeconds(1))) {
            final Future<ReturnCode> f = e.submit(() -> d.apply(lifecycle)); // will block
            Assertions.assertThatThrownBy(() -> f.get(1, TimeUnit.SECONDS))
                    .isInstanceOf(TimeoutException.class);
            lifecycle.resumeToShutdown(); // unblock
            Assertions.assertThat(f.get()).isEqualTo(ReturnCode.OK); // should now finish
            Mockito.verify(p).run();
            Mockito.verify(bau).run();
        } finally {
            e.shutdownNow();
        }
        Mockito.verify(m).close();
    }

    @AfterEach
    public void cleanup() {
        final ShutdownHook.Result r = new ShutdownHook.Result(ReturnCode.OK, null);
        lifecycle.getShutdownHooks().forEach(h -> h.get().ifPresent(s -> s.accept(r)));
    }
}
