/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.notifications.listeners;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.*;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.ExtendedPortfolioOverview;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.extensions.ListenerServiceLoader;
import com.github.robozonky.notifications.*;
import com.github.robozonky.notifications.templates.TemplateProcessor;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import com.github.robozonky.test.mock.MockInvestmentBuilder;
import com.github.robozonky.test.mock.MockLoanBuilder;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.Mockito.*;

public class AbstractListenerTest extends AbstractRoboZonkyTest {

    private static final RoboZonkyTestingEvent EVENT = OffsetDateTime::now;
    private static final ExtendedPortfolioOverview MAX_PORTFOLIO = mockPortfolioOverview();
    private static final SessionInfo SESSION_INFO = new SessionInfo("someone@somewhere.net");

    private static AbstractListener<? extends Event> getListener(final SupportedListener s,
                                                                 final AbstractTargetHandler p) {
        final AbstractListener<? extends Event> e = spy((AbstractListener<? extends Event>) s.getListener(p));
        // always return a listener that WILL send an e-mail, even though this means shouldSendEmail() is not tested
        doReturn(true).when(e).shouldNotify(any(), eq(SESSION_INFO));
        return e;
    }

    private static <T extends Event> void testFormal(final AbstractListener<T> listener, final T event,
                                                     final SupportedListener listenerType) {
        assertThat(event).isInstanceOf(listenerType.getEventType());
        assertThat(listener.getTemplateFileName())
                .isNotNull()
                .isNotEmpty();
    }

    private <T extends Event> void testPlainTextProcessing(final AbstractListener<T> listener, final T event) throws IOException, TemplateException {
        final String s = TemplateProcessor.INSTANCE.processPlainText(listener.getTemplateFileName(),
                                                                     listener.getData(event, SESSION_INFO));
        logger.debug("Plain text was: {}.", s);
        assertThat(s).contains(Defaults.ROBOZONKY_URL);
        assertThat(s).contains("uživatel"); // check that UTF-8 is properly encoded
    }

    private static void testListenerEnabled(final Event event) {
        final NotificationListenerService service = new NotificationListenerService();
        final Stream<? extends EventListenerSupplier<? extends Event>> supplier =
                service.findListeners(event.getClass());
        assertThat(supplier).isNotEmpty();
    }

    private static AbstractTargetHandler getHandler(final ConfigStorage storage) {
        return spy(new TestingTargetHandler(storage));
    }

    static AbstractTargetHandler getHandler() throws IOException {
        final ConfigStorage cs =
                ConfigStorage.create(AbstractListenerTest.class.getResourceAsStream("notifications-enabled.cfg"));
        return getHandler(cs);
    }

    private static <T extends Event> void testTriggered(final AbstractTargetHandler h,
                                                        final AbstractListener<T> listener,
                                                        final T event) throws Exception {
        listener.handle(event, SESSION_INFO);
        verify(h, times(1)).send(eq(SESSION_INFO), notNull(), notNull(), notNull());
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> DynamicContainer forListener(final SupportedListener listener,
                                                                  final T e) throws IOException {
        final AbstractTargetHandler p = getHandler();
        final AbstractListener<T> l = (AbstractListener<T>) getListener(listener, p);
        return DynamicContainer.dynamicContainer(listener.toString(), Stream.of(
                dynamicTest("is formally correct", () -> testFormal(l, e, listener)),
                dynamicTest("is processed as plain text", () -> testPlainTextProcessing(l, e)),
                dynamicTest("is processed as HTML", () -> testHtmlProcessing(l, e)),
                dynamicTest("triggers the sending code", () -> testTriggered(p, l, e)),
                dynamicTest("has listener enabled", () -> testListenerEnabled(e))
        ));
    }

    private <T extends Event> void testHtmlProcessing(final AbstractListener<T> listener,
                                                      final T event) throws IOException, TemplateException {
        final Map<String, Object> data = new HashMap<>(listener.getData(event, SESSION_INFO));
        data.put("subject", UUID.randomUUID().toString());
        final String s = TemplateProcessor.INSTANCE.processHtml(listener.getTemplateFileName(),
                                                                Collections.unmodifiableMap(data));
        logger.debug("HTML text was: {}.", s);
        assertThat(s).contains(Defaults.ROBOZONKY_URL);
        assertThat(s).contains("uživatel"); // check that UTF-8 is properly encoded
    }

    @BeforeEach
    void configureNotifications() throws URISyntaxException, MalformedURLException {
        ListenerServiceLoader.registerConfiguration(SESSION_INFO,
                                                    AbstractListenerTest.class.getResource(
                                                            "notifications-enabled.cfg").toURI().toURL());
    }

    @Test
    void spamProtectionAvailable() throws Exception {
        final ConfigStorage cs =
                ConfigStorage.create(getClass().getResourceAsStream("notifications-enabled-spamless.cfg"));
        final AbstractTargetHandler p = getHandler(cs);
        final TestingEmailingListener l = new TestingEmailingListener(p);
        l.handle(EVENT, SESSION_INFO);
        verify(p, times(1)).send(notNull(), notNull(), notNull(), notNull());
        l.handle(EVENT, SESSION_INFO);
        // e-mail not re-sent, finisher not called again
        verify(p, times(1)).send(notNull(), notNull(), notNull(), notNull());
    }

    @BeforeEach
    @AfterEach
    @Override
    protected void deleteState() { // JUnit 5 won't invoke this from an abstract class
        super.deleteState();
    }

    @TestFactory
    Stream<DynamicNode> listeners() throws IOException {
        // prepare data
        final Loan loan = new MockLoanBuilder()
                .setAmount(100_000)
                .setAnnuity(BigDecimal.TEN)
                .setInterestRate(Ratio.ONE)
                .setDatePublished(OffsetDateTime.now().minusMonths(2))
                .setName("Úvěr")
                .setRegion(Region.JIHOCESKY)
                .setPurpose(Purpose.AUTO_MOTO)
                .setMainIncomeType(MainIncomeType.EMPLOYMENT)
                .setNonReservedRemainingInvestment(2000)
                .setRating(Rating.AAAAA)
                .setTermInMonths(25)
                .setUrl(new URL("http://www.robozonky.cz"))
                .build();
        final LoanDescriptor loanDescriptor = new LoanDescriptor(loan);
        final RecommendedLoan recommendation = loanDescriptor.recommend(1200).get();
        final Investment i = MockInvestmentBuilder.fresh(loan, 1000)
                .setExpectedInterest(BigDecimal.TEN)
                .setPaidPenalty(BigDecimal.ZERO)
                .setInvestmentDate(OffsetDateTime.now())
                .build();
        // create events for listeners
        return Stream.of(
                forListener(SupportedListener.RESERVATION_ACCEPTED, new MyReservationAcceptedEvent(loan, i)),
                forListener(SupportedListener.INVESTMENT_MADE, new MyInvestmentMadeEvent(loan, i)),
                forListener(SupportedListener.INVESTMENT_SOLD, new MyInvestmentSoldEvent(loan, i)),
                forListener(SupportedListener.LOAN_NO_LONGER_DELINQUENT, new MyLoanNoLongerDelinquentEvent(loan, i)),
                forListener(SupportedListener.LOAN_DEFAULTED, new MyLoanDefaultedEvent(loan, i)),
                forListener(SupportedListener.LOAN_LOST, new MyLoanLostEvent(loan, i)),
                forListener(SupportedListener.LOAN_NOW_DELINQUENT, new MyLoanNowDelinquent(loan, i)),
                forListener(SupportedListener.LOAN_DELINQUENT_10_PLUS, new MyLoanDelinquent10Plus(loan, i)),
                forListener(SupportedListener.LOAN_DELINQUENT_30_PLUS, new MyLoanDelinquent30Plus(loan, i)),
                forListener(SupportedListener.LOAN_DELINQUENT_60_PLUS, new MyLoanDelinquent60Plus(loan, i)),
                forListener(SupportedListener.LOAN_DELINQUENT_90_PLUS, new MyLoanDelinquent90Plus(loan, i)),
                forListener(SupportedListener.WEEKLY_SUMMARY, new MyWeeklySummaryEvent()),
                forListener(SupportedListener.DAEMON_SUSPENDED, new MyRoboZonkyDaemonSuspendedEvent()),
                forListener(SupportedListener.DAEMON_RESUMED, new MyRoboZonkyDaemonResumedEvent()),
                forListener(SupportedListener.CRASHED, new MyRoboZonkyCrashedEvent()),
                forListener(SupportedListener.INITIALIZED, (RoboZonkyInitializedEvent) OffsetDateTime::now),
                forListener(SupportedListener.ENDING, (RoboZonkyEndingEvent) OffsetDateTime::now),
                forListener(SupportedListener.TESTING, (RoboZonkyTestingEvent) OffsetDateTime::now),
                forListener(SupportedListener.UPDATE_DETECTED, new MyRoboZonkyUpdateDetectedEvent()),
                forListener(SupportedListener.EXPERIMENTAL_UPDATE_DETECTED,
                            new MyRoboZonkyExperimentalUpdateDetectedEvent()),
                forListener(SupportedListener.INVESTMENT_PURCHASED, new MyInvestmentPurchasedEvent(loan, i)),
                forListener(SupportedListener.SALE_OFFERED, new MySaleOfferedEvent(i, loan))
        );
    }

    private static class TestingTargetHandler extends AbstractTargetHandler {

        public TestingTargetHandler(final ConfigStorage storage) {
            super(storage, Target.EMAIL);
        }

        @Override
        public void send(final SessionInfo sessionInfo, final String subject, final String message,
                         final String fallbackMessage) {

        }
    }

    private static final class TestingEmailingListener extends AbstractListener<RoboZonkyTestingEvent> {

        TestingEmailingListener(final AbstractTargetHandler handler) {
            super(SupportedListener.TESTING, handler);
        }

        @Override
        String getSubject(final RoboZonkyTestingEvent event) {
            return "No actual subject";
        }

        @Override
        String getTemplateFileName() {
            return "testing.ftl";
        }
    }

    private static class MyRoboZonkyExperimentalUpdateDetectedEvent implements RoboZonkyExperimentalUpdateDetectedEvent {

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public String getNewVersion() {
            return "1.3.0-beta-1";
        }
    }

    private static class MyRoboZonkyUpdateDetectedEvent implements RoboZonkyUpdateDetectedEvent {

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public String getNewVersion() {
            return "1.2.3";
        }
    }

    private static class MyRoboZonkyDaemonSuspendedEvent implements RoboZonkyDaemonSuspendedEvent {

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public Throwable getCause() {
            return new RuntimeException();
        }
    }

    private static class MyRoboZonkyDaemonResumedEvent implements RoboZonkyDaemonResumedEvent {

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public OffsetDateTime getUnavailableSince() {
            return OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
        }

        @Override
        public OffsetDateTime getUnavailableUntil() {
            return getUnavailableSince()
                    .plusDays(1)
                    .minusSeconds(1);
        }
    }

    private static class MyRoboZonkyCrashedEvent implements RoboZonkyCrashedEvent {

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public Optional<Throwable> getCause() {
            return Optional.of(new OutOfMemoryError());
        }
    }

    private static class MyInvestmentPurchasedEvent implements InvestmentPurchasedEvent {

        private final Loan loan;
        private final Investment i;

        public MyInvestmentPurchasedEvent(final Loan loan, final Investment i) {
            this.loan = loan;
            this.i = i;
        }

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public Loan getLoan() {
            return loan;
        }

        @Override
        public Investment getInvestment() {
            return i;
        }

        @Override
        public PortfolioOverview getPortfolioOverview() {
            return MAX_PORTFOLIO;
        }
    }

    private static class MySaleOfferedEvent implements SaleOfferedEvent {

        private final Investment i;
        private final Loan loan;

        public MySaleOfferedEvent(final Investment i, final Loan loan) {
            this.i = i;
            this.loan = loan;
        }

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public Loan getLoan() {
            return loan;
        }

        @Override
        public Investment getInvestment() {
            return i;
        }
    }

    private static class MyReservationAcceptedEvent implements ReservationAcceptedEvent {

        private final Loan loan;
        private final Investment i;

        public MyReservationAcceptedEvent(final Loan loan, final Investment i) {
            this.loan = loan;
            this.i = i;
        }

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public Loan getLoan() {
            return loan;
        }

        @Override
        public Investment getInvestment() {
            return i;
        }

        @Override
        public PortfolioOverview getPortfolioOverview() {
            return MAX_PORTFOLIO;
        }
    }

    private static class MyInvestmentMadeEvent implements InvestmentMadeEvent {

        private final Loan loan;
        private final Investment i;

        public MyInvestmentMadeEvent(final Loan loan, final Investment i) {
            this.loan = loan;
            this.i = i;
        }

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public Loan getLoan() {
            return loan;
        }

        @Override
        public Investment getInvestment() {
            return i;
        }

        @Override
        public PortfolioOverview getPortfolioOverview() {
            return MAX_PORTFOLIO;
        }
    }

    private static class MyInvestmentSoldEvent implements InvestmentSoldEvent {

        private final Loan loan;
        private final Investment i;

        public MyInvestmentSoldEvent(final Loan loan, final Investment i) {
            this.loan = loan;
            this.i = i;
        }

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public Loan getLoan() {
            return loan;
        }

        @Override
        public Investment getInvestment() {
            return i;
        }

        @Override
        public PortfolioOverview getPortfolioOverview() {
            return MAX_PORTFOLIO;
        }
    }

    private static class MyLoanNoLongerDelinquentEvent implements LoanNoLongerDelinquentEvent {

        private final Loan loan;
        private final Investment i;

        public MyLoanNoLongerDelinquentEvent(final Loan loan, final Investment i) {
            this.loan = loan;
            this.i = i;
        }

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public Loan getLoan() {
            return loan;
        }

        @Override
        public Investment getInvestment() {
            return i;
        }
    }

    private static class MyLoanDefaultedEvent implements LoanDefaultedEvent {

        private final Loan loan;
        private final Investment i;

        public MyLoanDefaultedEvent(final Loan loan, final Investment i) {
            this.loan = loan;
            this.i = i;
        }

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public Loan getLoan() {
            return loan;
        }

        @Override
        public Investment getInvestment() {
            return i;
        }

        @Override
        public LocalDate getDelinquentSince() {
            return LocalDate.now();
        }

        @Override
        public Collection<Development> getCollectionActions() {
            return Collections.emptyList();
        }
    }

    private static class MyLoanDelinquent10Plus implements LoanDelinquent10DaysOrMoreEvent {

        private final Loan loan;
        private final Investment i;

        public MyLoanDelinquent10Plus(final Loan loan, final Investment i) {
            this.loan = loan;
            this.i = i;
        }

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public Loan getLoan() {
            return loan;
        }

        @Override
        public Investment getInvestment() {
            return i;
        }

        @Override
        public LocalDate getDelinquentSince() {
            return LocalDate.now();
        }

        @Override
        public Collection<Development> getCollectionActions() {
            return Collections.emptyList();
        }

        @Override
        public int getThresholdInDays() {
            return 10;
        }
    }

    private static class MyLoanDelinquent30Plus implements LoanDelinquent30DaysOrMoreEvent {

        private final Loan loan;
        private final Investment i;

        public MyLoanDelinquent30Plus(final Loan loan, final Investment i) {
            this.loan = loan;
            this.i = i;
        }

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public Loan getLoan() {
            return loan;
        }

        @Override
        public Investment getInvestment() {
            return i;
        }

        @Override
        public LocalDate getDelinquentSince() {
            return LocalDate.now();
        }

        @Override
        public Collection<Development> getCollectionActions() {
            return Collections.emptyList();
        }

        @Override
        public int getThresholdInDays() {
            return 30;
        }
    }

    private static class MyLoanDelinquent60Plus implements LoanDelinquent60DaysOrMoreEvent {

        private final Loan loan;
        private final Investment i;

        public MyLoanDelinquent60Plus(final Loan loan, final Investment i) {
            this.loan = loan;
            this.i = i;
        }

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public Loan getLoan() {
            return loan;
        }

        @Override
        public Investment getInvestment() {
            return i;
        }

        @Override
        public LocalDate getDelinquentSince() {
            return LocalDate.now();
        }

        @Override
        public Collection<Development> getCollectionActions() {
            return Collections.emptyList();
        }

        @Override
        public int getThresholdInDays() {
            return 60;
        }
    }

    private static class MyLoanDelinquent90Plus implements LoanDelinquent90DaysOrMoreEvent {

        private final Loan loan;
        private final Investment i;

        public MyLoanDelinquent90Plus(final Loan loan, final Investment i) {
            this.loan = loan;
            this.i = i;
        }

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public Loan getLoan() {
            return loan;
        }

        @Override
        public Investment getInvestment() {
            return i;
        }

        @Override
        public LocalDate getDelinquentSince() {
            return LocalDate.now();
        }

        @Override
        public Collection<Development> getCollectionActions() {
            return Collections.emptyList();
        }

        @Override
        public int getThresholdInDays() {
            return 90;
        }
    }

    private static class MyLoanNowDelinquent implements LoanNowDelinquentEvent {

        private final Loan loan;
        private final Investment i;

        public MyLoanNowDelinquent(final Loan loan, final Investment i) {
            this.loan = loan;
            this.i = i;
        }

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public Loan getLoan() {
            return loan;
        }

        @Override
        public Investment getInvestment() {
            return i;
        }

        @Override
        public LocalDate getDelinquentSince() {
            return LocalDate.now();
        }

        @Override
        public Collection<Development> getCollectionActions() {
            return Collections.emptyList();
        }

        @Override
        public int getThresholdInDays() {
            return 0;
        }
    }

    private static class MyLoanLostEvent implements LoanLostEvent {

        private final Loan loan;
        private final Investment i;

        public MyLoanLostEvent(final Loan loan, final Investment i) {
            this.loan = loan;
            this.i = i;
        }

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public Loan getLoan() {
            return loan;
        }

        @Override
        public Investment getInvestment() {
            return i;
        }
    }

    private static class MyWeeklySummaryEvent implements WeeklySummaryEvent {

        @Override
        public OffsetDateTime getCreatedOn() {
            return OffsetDateTime.now();
        }

        @Override
        public ExtendedPortfolioOverview getPortfolioOverview() {
            return mockPortfolioOverview();
        }
    }
}
