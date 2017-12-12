/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock.protocol;

import java.util.Random;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCSession;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCTunnelManager;
import org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PCCSessionListener implements PCEPSessionListener, PCCSession {

    private static final Logger LOG = LoggerFactory.getLogger(PCCSessionListener.class);

    private final boolean errorMode;
    private final PCCTunnelManager tunnelManager;
    private final int sessionId;
    private PCEPSession session;

    public PCCSessionListener(final int sessionId, final PCCTunnelManager tunnelManager, final boolean errorMode) {
        this.errorMode = errorMode;
        this.tunnelManager = tunnelManager;
        this.sessionId = sessionId;
    }

    @Override
    public void onMessage(final PCEPSession session, final Message message) {
        LOG.trace("Received message: {}", message);
        if (this.errorMode) {
            //random error message
            session.sendMessage(createErrorMessage(message));
            return;
        }
        if (message instanceof Pcupd) {
            final Updates upd = ((Pcupd) message).getPcupdMessage().getUpdates().get(0);
            this.tunnelManager.onMessagePcupd(upd, this);
        } else if (message instanceof Pcinitiate) {
            this.tunnelManager.onMessagePcInitiate(((Pcinitiate) message).getPcinitiateMessage().getRequests().get(0), this);
        }
    }

    @Override
    public void onSessionUp(final PCEPSession session) {
        LOG.debug("Session up.");
        this.session = session;
        this.tunnelManager.onSessionUp(this);
    }

    @Override
    public void onSessionDown(final PCEPSession session, final Exception exception) {
        LOG.info("Session down with cause : {} or exception: {}", exception.getCause(), exception, exception);
        this.tunnelManager.onSessionDown(this);
        try {
            session.close();
        } catch (Exception ie) {
            LOG.warn("Error closing session", ie);
        }
    }

    @Override
    public void onSessionTerminated(final PCEPSession session, final PCEPTerminationReason cause) {
        LOG.info("Session terminated. Cause : {}", cause.toString());
    }

    @Override
    public void sendReport(final Pcrpt reportMessage) {
        this.session.sendMessage(reportMessage);
    }

    @Override
    public void sendError(final Pcerr errorMessage) {
        this.session.sendMessage(errorMessage);
    }

    @Override
    public int getId() {
        return this.sessionId;
    }

    @Override
    public Tlvs getRemoteTlvs() {
        return this.session.getRemoteTlvs();
    }

    @Override
    public Tlvs localSessionCharacteristics() {
        return this.session.localSessionCharacteristics();
    }

    private final Random rnd = new Random();

    private PCEPErrors getRandomError() {
        return PCEPErrors.values()[this.rnd.nextInt(PCEPErrors.values().length)];
    }

    private Pcerr createErrorMessage(final Message message) {
        final Srp srp;
        if (message instanceof Pcupd) {
            srp = ((Pcupd) message).getPcupdMessage().getUpdates().get(0).getSrp();
        } else {
            srp = ((Pcinitiate) message).getPcinitiateMessage().getRequests().get(0).getSrp();
        }
        return MsgBuilderUtil.createErrorMsg(getRandomError(), srp.getOperationId().getValue());
    }

}
