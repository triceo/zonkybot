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

package com.github.robozonky.notifications.email;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.mail.internet.MimeMessage;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.robozonky.api.notifications.InvestmentSkippedEvent;
import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanDelinquent10DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent30DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent60DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.notifications.LoanRepaidEvent;
import com.github.robozonky.api.notifications.RemoteOperationFailedEvent;
import com.github.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.robozonky.api.notifications.RoboZonkyExperimentalUpdateDetectedEvent;
import com.github.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.api.notifications.RoboZonkyUpdateDetectedEvent;
import com.github.robozonky.api.notifications.SaleOfferedEvent;
import com.github.robozonky.api.notifications.SessionInfo;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import com.github.robozonky.util.Refreshable;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import freemarker.template.TemplateException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class EmailingListenerTest extends AbstractRoboZonkyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailingListenerTest.class);
    private static final RoboZonkyTestingEvent EVENT = new RoboZonkyTestingEvent();

    private static final GreenMail EMAIL = new GreenMail(getServerSetup());
    private static final PortfolioOverview MAX_PORTFOLIO = mockPortfolio(Integer.MAX_VALUE);

    private static ServerSetup getServerSetup() {
        final ServerSetup setup = ServerSetupTest.SMTP;
        setup.setServerStartupTimeout(5000);
        setup.setVerbose(true);
        return setup;
    }

    private static NotificationProperties getNotificationProperties() {
        final Refreshable<NotificationProperties> r = new RefreshableNotificationProperties();
        r.run();
        return r.get().get();
    }

    private static PortfolioOverview mockPortfolio(final int balance) {
        final PortfolioOverview portfolioOverview = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolioOverview.getCzkAvailable()).thenReturn(balance);
        return portfolioOverview;
    }

    private static AbstractEmailingListener<Event> getListener(final SupportedListener s,
                                                               final NotificationProperties p) {
        final AbstractEmailingListener<Event> e = Mockito.spy((AbstractEmailingListener<Event>) s.getListener(p));
        // always return a listener that WILL send an e-mail, even though this means shouldSendEmail() is not tested
        Mockito.doReturn(true).when(e).shouldSendEmail(ArgumentMatchers.any());
        return e;
    }

    private DynamicContainer forListener(final SupportedListener listener, final Event e) {
        final NotificationProperties p = getNotificationProperties();
        final AbstractEmailingListener<Event> l = getListener(listener, p);
        return DynamicContainer.dynamicContainer(listener.toString(), Stream.of(
                dynamicTest("is formally correct", () -> testFormal(l, e, listener)),
                dynamicTest("is processed correctly", () -> testProcessingWithoutErrors(l, e)),
                dynamicTest("sends email", () -> {
                    BalanceTracker.INSTANCE.reset();  // dynamic tests in JUnit don't call before/after methods !!!
                    testMailSent(l, e);
                }),
                dynamicTest("has listener enabled", () -> testListenerEnabled(e))
        ));
    }

    private static void testMailSent(final AbstractEmailingListener<Event> listener,
                                     final Event event) throws Exception {
        final int originalMessages = EMAIL.getReceivedMessages().length;
        listener.handle(event, new SessionInfo("someone@somewhere.net"));
        Assertions.assertThat(EMAIL.getReceivedMessages()).hasSize(originalMessages + 1);
        final MimeMessage m = EMAIL.getReceivedMessages()[originalMessages];
        Assertions.assertThat(m.getContentType()).contains(Defaults.CHARSET.displayName());
        Assertions.assertThat(m.getSubject()).isNotNull().isEqualTo(listener.getSubject(event));
        Assertions.assertThat(m.getFrom()[0].toString()).contains("user@seznam.cz");
    }

    private static void testFormal(final AbstractEmailingListener<Event> listener, final Event event,
                                   final SupportedListener listenerType) {
        Assertions.assertThat(event).isInstanceOf(listenerType.getEventType());
        Assertions.assertThat(listener.getTemplateFileName())
                .isNotNull()
                .isNotEmpty();
    }

    private static void testProcessingWithoutErrors(final AbstractEmailingListener<Event> listener,
                                                    final Event event) throws IOException, TemplateException {
        final String s = TemplateProcessor.INSTANCE.process(listener.getTemplateFileName(),
                                                            listener.getData(event, new SessionInfo(
                                                                    "someone@somewhere.net")));
        Assertions.assertThat(s).contains(Defaults.ROBOZONKY_URL);
    }

    private static void testListenerEnabled(final Event event) {
        final EmailListenerService service = new EmailListenerService();
        final EventListenerSupplier<?> supplier = service.findListener(event.getClass());
        Assertions.assertThat(supplier.get()).isPresent();
    }

    @BeforeEach
    void setProperty() {
        System.setProperty(RefreshableNotificationProperties.CONFIG_FILE_LOCATION_PROPERTY,
                           NotificationPropertiesTest.class.getResource("notifications-enabled.cfg").toString());
    }

    @Test
    void testSpamProtectionAvailable() throws IOException {
        this.deleteState(); // for some reason, JUnit 5 doesn't invoke this from the abstract parent
        final Properties props = new Properties();
        props.load(NotificationPropertiesTest.class.getResourceAsStream("notifications-enabled.cfg"));
        props.setProperty("hourlyMaxEmails", String.valueOf(1)); // spam protection
        final ListenerSpecificNotificationProperties p =
                new ListenerSpecificNotificationProperties(SupportedListener.TESTING,
                                                           new NotificationProperties(props));
        final Consumer<RoboZonkyTestingEvent> c = Mockito.mock(Consumer.class);
        final TestingEmailingListener l = new TestingEmailingListener(p);
        l.registerFinisher(c);
        Assertions.assertThat(l.countFinishers()).isEqualTo(2); // both spam protection and custom finisher available
        l.handle(EVENT, new SessionInfo("someone@somewhere.net"));
        Assertions.assertThat(EMAIL.getReceivedMessages()).hasSize(1);
        l.handle(EVENT, new SessionInfo("someone@somewhere.net"));
        // e-mail not re-sent, finisher not called again
        Mockito.verify(c, Mockito.times(1)).accept(ArgumentMatchers.any());
        Assertions.assertThat(EMAIL.getReceivedMessages()).hasSize(1);
    }

    @BeforeEach
    void startEmailing() {
        EMAIL.start();
        LOGGER.info("Started e-mailing.");
    }

    @AfterEach
    void stopEmailing() {
        LOGGER.info("Stopping e-mailing.");
        try {
            EMAIL.purgeEmailFromAllMailboxes();
            EMAIL.stop();
        } catch (final Exception ex) {
            LOGGER.warn("Failed stopping e-mail server.", ex);
        }
    }

    @TestFactory
    Stream<DynamicNode> listeners() {
        // prepare data
        final Loan loan = Mockito.spy(new Loan(66666, 100000));
        Mockito.when(loan.getDatePublished()).thenReturn(OffsetDateTime.now().minusMonths(2));
        Mockito.when(loan.getName()).thenReturn("Úvěr");
        Mockito.when(loan.getRegion()).thenReturn(Region.JIHOCESKY);
        Mockito.when(loan.getPurpose()).thenReturn(Purpose.AUTO_MOTO);
        Mockito.when(loan.getMainIncomeType()).thenReturn(MainIncomeType.EMPLOYMENT);
        Mockito.when(loan.getRemainingInvestment()).thenReturn(2000.0);
        Mockito.when(loan.getRating()).thenReturn(Rating.AAAAA);
        Mockito.when(loan.getTermInMonths()).thenReturn(25);
        Mockito.when(loan.getUrl()).thenReturn("http://www.robozonky.cz/");
        final LoanDescriptor loanDescriptor = new LoanDescriptor(loan);
        final RecommendedLoan recommendation = loanDescriptor.recommend(1200, false).get();
        final Investment i = new Investment(loan, 1000);
        // create events for listeners
        return Stream.of(
                forListener(SupportedListener.INVESTMENT_DELEGATED,
                            new InvestmentDelegatedEvent(recommendation, "random")),
                forListener(SupportedListener.INVESTMENT_MADE, new InvestmentMadeEvent(i, loan, MAX_PORTFOLIO)),
                forListener(SupportedListener.INVESTMENT_SOLD, new InvestmentSoldEvent(i, loan, MAX_PORTFOLIO)),
                forListener(SupportedListener.INVESTMENT_SKIPPED, new InvestmentSkippedEvent(recommendation)),
                forListener(SupportedListener.INVESTMENT_REJECTED,
                            new InvestmentRejectedEvent(recommendation, "random")),
                forListener(SupportedListener.LOAN_NO_LONGER_DELINQUENT,
                            new LoanNoLongerDelinquentEvent(i, loan, LocalDate.now())),
                forListener(SupportedListener.LOAN_DEFAULTED, new LoanDefaultedEvent(i, loan, LocalDate.now())),
                forListener(SupportedListener.LOAN_NOW_DELINQUENT,
                            new LoanNowDelinquentEvent(i, loan, LocalDate.now())),
                forListener(SupportedListener.LOAN_DELINQUENT_10_PLUS,
                            new LoanDelinquent10DaysOrMoreEvent(i, loan, LocalDate.now().minusDays(11))),
                forListener(SupportedListener.LOAN_DELINQUENT_30_PLUS,
                            new LoanDelinquent30DaysOrMoreEvent(i, loan, LocalDate.now().minusDays(31))),
                forListener(SupportedListener.LOAN_DELINQUENT_60_PLUS,
                            new LoanDelinquent60DaysOrMoreEvent(i, loan, LocalDate.now().minusDays(61))),
                forListener(SupportedListener.LOAN_DELINQUENT_90_PLUS,
                            new LoanDelinquent90DaysOrMoreEvent(i, loan, LocalDate.now().minusDays(91))),
                forListener(SupportedListener.LOAN_REPAID, new LoanRepaidEvent(i, loan, MAX_PORTFOLIO)),
                forListener(SupportedListener.BALANCE_ON_TARGET,
                            new ExecutionStartedEvent(Collections.emptyList(), MAX_PORTFOLIO)),
                forListener(SupportedListener.BALANCE_UNDER_MINIMUM,
                            new ExecutionStartedEvent(Collections.emptyList(), mockPortfolio(0))),
                forListener(SupportedListener.CRASHED,
                            new RoboZonkyCrashedEvent(ReturnCode.ERROR_UNEXPECTED, new RuntimeException())),
                forListener(SupportedListener.REMOTE_OPERATION_FAILED,
                            new RemoteOperationFailedEvent(new RuntimeException())),
                forListener(SupportedListener.DAEMON_FAILED, new RoboZonkyDaemonFailedEvent(new RuntimeException())),
                forListener(SupportedListener.INITIALIZED, new RoboZonkyInitializedEvent()),
                forListener(SupportedListener.ENDING, new RoboZonkyEndingEvent()),
                forListener(SupportedListener.TESTING, new RoboZonkyTestingEvent()),
                forListener(SupportedListener.UPDATE_DETECTED, new RoboZonkyUpdateDetectedEvent("1.2.3")),
                forListener(SupportedListener.EXPERIMENTAL_UPDATE_DETECTED,
                            new RoboZonkyExperimentalUpdateDetectedEvent("1.3.0-beta-1")),
                forListener(SupportedListener.INVESTMENT_PURCHASED,
                            new InvestmentPurchasedEvent(i, loan, MAX_PORTFOLIO)),
                forListener(SupportedListener.SALE_OFFERED, new SaleOfferedEvent(i, loan))
        );
    }

    private static final class TestingEmailingListener extends AbstractEmailingListener<RoboZonkyTestingEvent> {

        public TestingEmailingListener(final ListenerSpecificNotificationProperties properties) {
            super(properties);
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
}

