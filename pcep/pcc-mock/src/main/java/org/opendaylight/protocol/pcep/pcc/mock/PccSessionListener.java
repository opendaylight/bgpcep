/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import java.util.Random;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.pcep.pcc.mock.api.PccSession;
import org.opendaylight.protocol.pcep.pcc.mock.api.PccTunnelManager;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Srp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PccSessionListener implements PCEPSessionListener, PccSession {

    private static final Logger LOG = LoggerFactory.getLogger(PccSessionListener.class);

    private final boolean errorMode;
    private final PccTunnelManager tunnelManager;
    private final int sessionId;
    private PCEPSession session;

    public PccSessionListener(final int sessionId, final PccTunnelManager tunnelManager, final boolean errorMode) {
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
            final Updates update = ((Pcupd) message).getPcupdMessage().getUpdates().get(0);
            if (update.getLsp().isDelegate() != null && update.getLsp().isDelegate()) {
                //regular LSP update
                this.tunnelManager.reportToAll(update, this);
            } else {
                //returning LSP delegation
                this.tunnelManager.returnDelegation(update, this);
            }
        } else if (message instanceof Pcinitiate) {
            final Requests request = ((Pcinitiate) message).getPcinitiateMessage().getRequests().get(0);
            if (request.getSrp().getAugmentation(Srp1.class) != null && request.getSrp().getAugmentation(Srp1.class).isRemove()) {
                //remove LSP
                this.tunnelManager.removeTunnel(request, this);
            } else if (request.getLsp().isDelegate() != null && request.getLsp().isDelegate() && request.getEndpointsObj() == null) {
                //take LSP delegation
                this.tunnelManager.takeDelegation(request, this);
            } else {
                //create LSP
                this.tunnelManager.addTunnel(request, this);
            }
        }
    }

    @Override
    public void onSessionUp(final PCEPSession session) {
        LOG.debug("Session up.");
        this.session = session;
        this.tunnelManager.onSessionUp(this);
    }

    @Override
    public void onSessionDown(final PCEPSession session, final Exception e) {
        LOG.info("Session down with cause : {} or exception: {}", e.getCause(), e, e);
        this.tunnelManager.onSessionDown(this);
        session.close();
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
