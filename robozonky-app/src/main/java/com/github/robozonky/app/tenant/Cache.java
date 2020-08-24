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

package com.github.robozonky.app.tenant;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.internal.util.functional.Either;
import com.github.robozonky.internal.util.functional.Tuple;
import com.github.robozonky.internal.util.functional.Tuple2;

final class Cache<T> {

    private static final Logger LOGGER = LogManager.getLogger(Cache.class);

    private static final Backend<Loan> LOAN_BACKEND = new Backend<>() {
        @Override
        public Duration getEvictEvery() {
            return Duration.ofHours(1);
        }

        @Override
        public Duration getEvictAfter() {
            return Duration.ofDays(1);
        }

        @Override
        public Class<Loan> getItemClass() {
            return Loan.class;
        }

        @Override
        public Either<Exception, Loan> getItem(final long id, final Tenant tenant) {
            try { // TODO convert loan IDs to longs to get rid of the cast.
                return Either.right(tenant.call(zonky -> zonky.getLoan((int) id)));
            } catch (final Exception ex) {
                return Either.left(ex);
            }
        }

        @Override
        public boolean shouldCache(final Loan item) {
            return item.getRemainingInvestment()
                .isZero();
        }
    };

    private static final Backend<Investment> INVESTMENT_BACKEND = new Backend<>() {
        @Override
        public Duration getEvictEvery() {
            return Duration.ofHours(1);
        }

        @Override
        public Duration getEvictAfter() {
            return Duration.ofHours(1);
        }

        @Override
        public Class<Investment> getItemClass() {
            return Investment.class;
        }

        @Override
        public Either<Exception, Investment> getItem(final long id, final Tenant tenant) {
            try {
                return tenant.call(zonky -> zonky.getInvestment(id))
                        .map((Function<Investment, Either<Exception, Investment>>) Either::right)
                        .orElse(Either.left(new IllegalStateException("No investment #" + id)));
            } catch (final Exception ex) {
                return Either.left(ex);
            }
        }

        @Override
        public boolean shouldCache(final Investment item) {
            return item.getSmpSellInfo()
                .flatMap(si -> si.getFee()
                    .getExpiresAt()
                    .map(expiration -> expiration.isAfter(DateUtil.offsetNow())))
                .orElse(true);
        }
    };

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Tenant tenant;
    private final Backend<T> backend;
    private final Map<Long, Tuple2<T, Instant>> storage = new ConcurrentHashMap<>(20);
    private final Executor evictor;

    private Cache(final Tenant tenant, final Backend<T> backend) {
        LOGGER.debug("Starting {} cache for {}.", backend.getItemClass(), tenant);
        this.tenant = tenant;
        this.backend = backend;
        this.evictor = backend.startEviction(this::evict);
    }

    public static Cache<Loan> forLoan(final Tenant tenant) {
        return new Cache<>(tenant, LOAN_BACKEND);
    }

    public static Cache<Investment> forInvestment(final Tenant tenant) {
        return new Cache<>(tenant, INVESTMENT_BACKEND);
    }

    private static String identify(final Class<?> clz, final long id) {
        return clz.getCanonicalName() + " #" + id;
    }

    private boolean isExpired(final Tuple2<T, Instant> p) {
        final Instant now = DateUtil.now();
        final Instant expiration = p._2()
            .plus(backend.getEvictAfter());
        return expiration.isBefore(now);
    }

    private void evict() {
        LOGGER.trace("Evicting {}, total: {}.", backend.getItemClass(), storage.size());
        final long evictedCount = storage.entrySet()
            .stream()
            .filter(e -> isExpired(e.getValue()))
            .peek(e -> storage.remove(e.getKey()))
            .count();
        LOGGER.trace("Evicted {} items.", evictedCount);
    }

    Optional<T> getFromCache(final long id) {
        final Tuple2<T, Instant> result = storage.get(id);
        if (result == null || isExpired(result)) {
            LOGGER.trace("Miss for {}.", identify(id));
            return Optional.empty();
        } else {
            LOGGER.trace("Hit for {}.", identify(id));
            return Optional.of(result._1());
        }
    }

    private String identify(final long id) {
        return identify(backend.getItemClass(), id);
    }

    private void add(final long id, final T item) {
        storage.put(id, Tuple.of(item, DateUtil.now()));
    }

    public T get(final long id) {
        if (isClosed.get()) {
            throw new IllegalStateException("Already closed.");
        }
        return getFromCache(id).orElseGet(() -> {
            final T item = backend.getItem(id, tenant)
                .getOrElseThrow(e -> new IllegalStateException("Can not read " + identify(id) + " from Zonky.", e));
            if (backend.shouldCache(item)) {
                add(id, item);
            } else {
                // prevent caching information which will soon be outdated
                LOGGER.debug("Not adding {} as it is not yet fully invested.", identify(id));
            }
            return item;
        });
    }

    /**
     * For testing purposes only.
     */
    Executor getEvictor() {
        return evictor;
    }

    private interface Backend<I> {

        Duration getEvictEvery();

        Duration getEvictAfter();

        Class<I> getItemClass();

        Either<Exception, I> getItem(long id, Tenant tenant);

        boolean shouldCache(I item);

        default Executor startEviction(Runnable eviction) {
            var executor = CompletableFuture.delayedExecutor(getEvictEvery().toNanos(), TimeUnit.NANOSECONDS);
            executor.execute(() -> {
                try {
                    eviction.run();
                } finally {
                    executor.execute(eviction);
                }
            });
            return executor;
        }
    }

}
