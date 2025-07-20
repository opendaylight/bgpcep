/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCSession;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCTunnelManager;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Srp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PCCTunnelManagerImplTest {
    private static final InetAddress ADDRESS = InetAddresses.forString("1.2.4.5");
    private static final Timer TIMER = new HashedWheelTimer();
    private static final byte[] SYMBOLIC_NAME = "tets".getBytes(StandardCharsets.UTF_8);
    private static final Ero ERO = new EroBuilder().setSubobject(Lists.newArrayList(new SubobjectBuilder()
        .setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(new IpPrefixBuilder()
        .setIpPrefix(new IpPrefix(new Ipv4Prefix("127.0.0.2/32"))).build()).build()).build())).build();
    private final List<PCEPErrors> errorsSession1 = new ArrayList<>();
    private final List<PCEPErrors> errorsSession2 = new ArrayList<>();
    @Mock
    private PCCSession session1;
    @Mock
    private PCCSession session2;
    private final Optional<TimerHandler> timerHandler = Optional.empty();

    @Before
    public void setUp() {
        doNothing().when(session1).sendReport(any(Pcrpt.class));
        doAnswer(invocation -> {
            PCCTunnelManagerImplTest.this.errorsSession1.add(getError((Pcerr) invocation.getArguments()[0]));
            return null;
        }).when(session1).sendError(any(Pcerr.class));
        doReturn(0).when(session1).getId();
        doNothing().when(session2).sendReport(any(Pcrpt.class));
        doAnswer(invocation -> {
            PCCTunnelManagerImplTest.this.errorsSession2.add(getError((Pcerr) invocation.getArguments()[0]));
            return null;
        }).when(session2).sendError(any(Pcerr.class));
        doReturn(1).when(session2).getId();
    }

    @After
    public void tearDown() {
        errorsSession1.clear();
        errorsSession2.clear();
    }

    @Test
    public void testOnSessionUp() {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(1, ADDRESS, 0, 0, TIMER, timerHandler);
        checkSessionUp(session1, tunnelManager);
        checkSessionUp(session2, tunnelManager);
    }

    @Test
    public void testOnSessionDownAndDelegateBack() {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(1, ADDRESS, 1, 10, TIMER, timerHandler);
        checkSessionUp(session1, tunnelManager);
        checkSessionUp(session2, tunnelManager);
        checkSessionDown(session1, tunnelManager);
        tunnelManager.onSessionUp(session1);
        verify(session1, times(4)).sendReport(any(Pcrpt.class));
        verify(session2, times(2)).sendReport(any(Pcrpt.class));
    }

    private static void checkSessionDown(final PCCSession session, final PCCTunnelManager tunnelManager) {
        tunnelManager.onSessionDown(session);
        verify(session, times(2)).sendReport(any(Pcrpt.class));
    }

    private static void checkSessionUp(final PCCSession session, final PCCTunnelManager tunnelManager) {
        //1 reported LSP + 1 end-of-sync marker
        tunnelManager.onSessionUp(session);
        verify(session, times(2)).sendReport(any(Pcrpt.class));
    }

    @Test
    public void testOnSessionDownAndDelegateToOther() throws InterruptedException {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(1, ADDRESS, 0, -1, TIMER, timerHandler);
        tunnelManager.onSessionUp(session2);
        checkSessionUp(session1, tunnelManager);
        checkSessionDown(session1, tunnelManager);
        //wait for re-delegation timeout expires
        Thread.sleep(500);
        verify(session2, times(3)).sendReport(any(Pcrpt.class));
        tunnelManager.onSessionUp(session1);
        verify(session1, times(4)).sendReport(any(Pcrpt.class));
    }

    @Test
    public void testReportToAll() {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(1, ADDRESS, 0, 0, TIMER, timerHandler);
        tunnelManager.onSessionUp(session1);
        tunnelManager.onSessionUp(session2);
        tunnelManager.onMessagePcupd(createUpdateDelegate(1), session1);
        verify(session1, times(3)).sendReport(any(Pcrpt.class));
        verify(session2, times(3)).sendReport(any(Pcrpt.class));
    }

    @Test
    public void testReportToAllUnknownLsp() {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(1, ADDRESS, 0, 0, TIMER, timerHandler);
        tunnelManager.onSessionUp(session1);
        tunnelManager.onMessagePcupd(createUpdateDelegate(2), session1);
        verify(session1, times(1)).sendError(any(Pcerr.class));
        assertEquals(PCEPErrors.UNKNOWN_PLSP_ID, errorsSession1.get(0));
    }

    @Test
    public void testReportToAllNonDelegatedLsp() {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(1, ADDRESS, 0, 0, TIMER, timerHandler);
        tunnelManager.onSessionUp(session1);
        tunnelManager.onSessionUp(session2);
        tunnelManager.onMessagePcupd(createUpdateDelegate(1), session2);
        verify(session2, times(1)).sendError(any(Pcerr.class));
        assertEquals(PCEPErrors.UPDATE_REQ_FOR_NON_LSP, errorsSession2.get(0));
    }

    @Test
    public void testReturnDelegationPccLsp() throws InterruptedException {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(1, ADDRESS, 1, -1, TIMER, timerHandler);
        tunnelManager.onSessionUp(session1);
        tunnelManager.onSessionUp(session2);
        tunnelManager.onMessagePcupd(createUpdate(1), session1);
        verify(session1, times(3)).sendReport(any(Pcrpt.class));
        verify(session2, times(2)).sendReport(any(Pcrpt.class));
        //wait for re-delegation timer expires
        Thread.sleep(1200);
        verify(session2, times(3)).sendReport(any(Pcrpt.class));
    }

    @Test
    public void testReturnDelegationUnknownLsp() {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(1, ADDRESS, 0, 0, TIMER, timerHandler);
        tunnelManager.onSessionUp(session1);
        tunnelManager.onMessagePcupd(createUpdate(2), session1);
        verify(session1, times(1)).sendError(any(Pcerr.class));
        assertEquals(PCEPErrors.UNKNOWN_PLSP_ID, errorsSession1.get(0));
    }

    @Test
    public void testReturnDelegationNonDelegatedLsp() {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(1, ADDRESS, 0, 0, TIMER, timerHandler);
        tunnelManager.onSessionUp(session1);
        tunnelManager.onSessionUp(session2);
        tunnelManager.onMessagePcupd(createUpdate(1), session2);
        verify(session2, times(1)).sendError(any(Pcerr.class));
        assertEquals(PCEPErrors.UPDATE_REQ_FOR_NON_LSP, errorsSession2.get(0));
    }

    @Test
    public void testAddTunnel() {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(0, ADDRESS, 0, 0, TIMER, timerHandler);
        tunnelManager.onSessionUp(session1);
        tunnelManager.onSessionUp(session2);
        tunnelManager.onMessagePcInitiate(createRequests(1), session1);
        verify(session1, times(1)).sendReport(any(Pcrpt.class));
        verify(session2, times(1)).sendReport(any(Pcrpt.class));
    }

    @Test
    public void testRemoveTunnel() {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(0, ADDRESS, 0, 0, TIMER, timerHandler);
        tunnelManager.onSessionUp(session1);
        tunnelManager.onSessionUp(session2);
        tunnelManager.onMessagePcInitiate(createRequests(1), session1);
        tunnelManager.onMessagePcInitiate(createRequestsRemove(1), session1);
        verify(session1, times(2)).sendReport(any(Pcrpt.class));
        verify(session2, times(2)).sendReport(any(Pcrpt.class));
    }

    @Test
    public void testRemoveTunnelUnknownLsp() {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(0, ADDRESS, 0, 0, TIMER, timerHandler);
        tunnelManager.onSessionUp(session1);
        tunnelManager.onMessagePcInitiate(createRequestsRemove(1), session1);
        verify(session1, times(1)).sendError(any(Pcerr.class));
        assertEquals(PCEPErrors.UNKNOWN_PLSP_ID, errorsSession1.get(0));
    }

    @Test
    public void testRemoveTunnelNotPceInitiatedLsp() {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(1, ADDRESS, 0, 0, TIMER, timerHandler);
        tunnelManager.onSessionUp(session1);
        tunnelManager.onMessagePcInitiate(createRequestsRemove(1), session1);
        verify(session1, times(1)).sendError(any(Pcerr.class));
        assertEquals(PCEPErrors.LSP_NOT_PCE_INITIATED, errorsSession1.get(0));
    }

    @Test
    public void testRemoveTunnelNotDelegated() {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(0, ADDRESS, 0, 0, TIMER, timerHandler);
        tunnelManager.onSessionUp(session1);
        tunnelManager.onSessionUp(session2);
        tunnelManager.onMessagePcInitiate(createRequests(1), session1);
        tunnelManager.onMessagePcInitiate(createRequestsRemove(1), session2);
        verify(session2, times(1)).sendError(any(Pcerr.class));
        assertEquals(PCEPErrors.UPDATE_REQ_FOR_NON_LSP, errorsSession2.get(0));
    }

    @Test
    public void testTakeDelegation() throws InterruptedException {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(0, ADDRESS, 0, -1, TIMER, timerHandler);
        tunnelManager.onSessionUp(session1);
        tunnelManager.onSessionUp(session2);
        tunnelManager.onMessagePcInitiate(createRequests(1), session1); //AddTunel
        tunnelManager.onMessagePcupd(createUpdate(1), session1); //returnDelegation
        verify(session1, times(2)).sendReport(any(Pcrpt.class));
        verify(session2, times(1)).sendReport(any(Pcrpt.class));
        Thread.sleep(500);
        tunnelManager.onMessagePcInitiate(createRequestsDelegate(1), session2);//takeDelegation
        verify(session1, times(2)).sendReport(any(Pcrpt.class));
        verify(session2, times(2)).sendReport(any(Pcrpt.class));
    }

    @Test
    public void testTakeDelegationUnknownLsp() {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(0, ADDRESS, 0, 0, TIMER, timerHandler);
        tunnelManager.onSessionUp(session1);
        tunnelManager.onMessagePcInitiate(createRequestsDelegate(1), session1);
        verify(session1, times(1)).sendError(any(Pcerr.class));
        assertEquals(PCEPErrors.UNKNOWN_PLSP_ID, errorsSession1.get(0));
    }

    @Test
    public void testTakeDelegationNotPceInitiatedLsp() {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(1, ADDRESS, 0, 0, TIMER, timerHandler);
        tunnelManager.onSessionUp(session1);
        tunnelManager.onMessagePcInitiate(createRequestsDelegate(1), session1);
        verify(session1, times(1)).sendError(any(Pcerr.class));
        assertEquals(PCEPErrors.LSP_NOT_PCE_INITIATED, errorsSession1.get(0));
    }

    @Test
    public void testReturnDelegationNoRetake() throws InterruptedException {
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(0, ADDRESS, 0, 0, TIMER, timerHandler);
        tunnelManager.onSessionUp(session1);
        tunnelManager.onSessionUp(session2);
        tunnelManager.onMessagePcInitiate(createRequests(1), session1);
        tunnelManager.onMessagePcupd(createUpdate(1), session1);
        //wait for state timeout expires
        Thread.sleep(500);
        verify(session1, times(3)).sendReport(any(Pcrpt.class));
        verify(session2, times(2)).sendReport(any(Pcrpt.class));
    }

    private static Updates createUpdateDelegate(final long plspId) {
        return createUpdate(plspId, Optional.of(Boolean.TRUE));
    }

    private static Updates createUpdate(final long plspId) {
        return createUpdate(plspId, Optional.empty());
    }

    private static Updates createUpdate(final long plspId, final Optional<Boolean> delegate) {
        final LspBuilder lsp = new LspBuilder().setPlspId(new PlspId(Uint32.valueOf(plspId)));
        if (delegate.isPresent()) {
            lsp.setDelegate(Boolean.TRUE);
        }
        return new UpdatesBuilder()
            .setLsp(lsp.build())
            .setPath(new PathBuilder().setEro(ERO).build())
            .setSrp(new SrpBuilder().setOperationId(new SrpIdNumber(Uint32.ZERO)).build())
            .build();
    }

    private static Requests createRequests(final long plspId, final Optional<Boolean> remove,
            final Optional<Boolean> delegate) {
        final LspBuilder lsp = new LspBuilder().setTlvs(new TlvsBuilder()
            .setSymbolicPathName(new SymbolicPathNameBuilder().setPathName(
                new SymbolicPathName(SYMBOLIC_NAME)).build()).build()).setPlspId(new PlspId(Uint32.valueOf(plspId)));
        if (delegate.isPresent()) {
            lsp.setDelegate(Boolean.TRUE);
        }

        final SrpBuilder srpBuilder = new SrpBuilder();
        if (remove.isPresent()) {
            srpBuilder.addAugmentation(new Srp1Builder().setRemove(Boolean.TRUE).build());
        }
        return new RequestsBuilder()
            .setEro(ERO)
            .setLsp(lsp.build())
            .setSrp(srpBuilder.setOperationId(new SrpIdNumber(Uint32.ZERO)).build())
            .build();
    }

    private static Requests createRequests(final long plspId) {
        return createRequests(plspId, Optional.empty(), Optional.empty());
    }

    private static Requests createRequestsRemove(final long plspId) {
        return createRequests(plspId, Optional.of(Boolean.TRUE), Optional.empty());
    }

    private static Requests createRequestsDelegate(final long plspId) {
        return createRequests(plspId, Optional.empty(), Optional.of(Boolean.TRUE));
    }

    private static PCEPErrors getError(final Pcerr errorMessage) {
        final ErrorObject errorObject = errorMessage.getPcerrMessage().getErrors().get(0).getErrorObject();
        return PCEPErrors.forValue(errorObject.getType(), errorObject.getValue());
    }
}
