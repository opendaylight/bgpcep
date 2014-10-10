/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.createLsp;
import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.createLspTlvs;
import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.createPath;
import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.createPcRtpMessage;
import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.createSrp;
import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.updToRptPath;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleSessionListener implements PCEPSessionListener {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleSessionListener.class);

    private final int lspsCount;
    private final boolean pcError;
    private final InetAddress address;

    public SimpleSessionListener(final int lspsCount, final boolean pcError, final InetAddress address) {
        Preconditions.checkArgument(lspsCount > 0);
        this.lspsCount = lspsCount;
        this.pcError = pcError;
        this.address = address;
    }

    @Override
    public void onMessage(final PCEPSession session, final Message message) {
        LOG.trace("Received message: {}", message);
        if (message instanceof Pcupd) {
            final Pcupd updMsg = (Pcupd) message;
            final Updates updates = updMsg.getPcupdMessage().getUpdates().get(0);
            final long srpId = updates.getSrp().getOperationId().getValue();
            if (pcError) {
                session.sendMessage(MsgBuilderUtil.createErrorMsg(getRandomError(), srpId));
            } else {
                final Tlvs tlvs = createLspTlvs(updates.getLsp().getPlspId().getValue(), false,
                        getDestinationAddress(updates.getPath().getEro().getSubobject()), this.address, this.address);
                final Pcrpt pcRpt = createPcRtpMessage(new LspBuilder(updates.getLsp()).setTlvs(tlvs).build(),
                        Optional.fromNullable(createSrp(srpId)), updToRptPath(updates.getPath()));
                session.sendMessage(pcRpt);
            }
        }
    }

    @Override
    public void onSessionUp(final PCEPSession session) {
        LOG.debug("Session up.");
        for (int i = 1; i <= this.lspsCount; i++) {
            final Tlvs tlvs = MsgBuilderUtil.createLspTlvs(i, true, this.address, this.address,
                    this.address);
            session.sendMessage(createPcRtpMessage(
                    createLsp(i, true, Optional.<Tlvs> fromNullable(tlvs)), Optional.<Srp> absent(),
                    createPath(Collections.<Subobject> emptyList())));
        }
        // end-of-sync marker
        session.sendMessage(createPcRtpMessage(createLsp(0, false, Optional.<Tlvs> absent()), Optional.<Srp> absent(),
                createPath(Collections.<Subobject> emptyList())));
    }

    @Override
    public void onSessionDown(final PCEPSession session, final Exception e) {
        LOG.info("Session down with cause : {} or exception: {}", e.getCause(), e, e);
        session.close();
    }

    @Override
    public void onSessionTerminated(final PCEPSession session, final PCEPTerminationReason cause) {
        LOG.info("Session terminated. Cause : {}", cause.toString());
    }

    private InetAddress getDestinationAddress(final List<Subobject> subobjects) {
        if (subobjects != null && !subobjects.isEmpty()) {
            final String prefix = ((IpPrefixCase) subobjects.get(subobjects.size() - 1).getSubobjectType())
                    .getIpPrefix().getIpPrefix().getIpv4Prefix().getValue();
            try {
                return InetAddress.getByName(prefix.substring(0, prefix.indexOf('/')));
            } catch (UnknownHostException e) {
                LOG.warn("Unknown host name {}", prefix);
            }
        }
        return this.address;
    }

    private Random rnd = new Random();

    private PCEPErrors getRandomError() {
        return PCEPErrors.values()[rnd.nextInt(PCEPErrors.values().length)];
    }

}
