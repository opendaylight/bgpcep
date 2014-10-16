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
import com.google.common.collect.Lists;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleSessionListener implements PCEPSessionListener {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleSessionListener.class);

    private static final String ENDPOINT_ADDRESS = "1.1.1.1";
    private static final String ENDPOINT_PREFIX = ENDPOINT_ADDRESS + "/32";

    private static final Subobject DEFAULT_ENDPOINT_HOP = getDefaultEROEndpointHop();

    private final int lspsCount;
    private final boolean pcError;
    private final String address;

    public SimpleSessionListener(final int lspsCount, final boolean pcError, final InetAddress address) {
        Preconditions.checkArgument(lspsCount > 0);
        this.lspsCount = lspsCount;
        this.pcError = pcError;
        this.address = address.getHostAddress();
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
            final Tlvs tlvs = MsgBuilderUtil.createLspTlvs(i, true, ENDPOINT_ADDRESS, this.address,
                    this.address);
            session.sendMessage(createPcRtpMessage(
                    createLsp(i, true, Optional.<Tlvs> fromNullable(tlvs)), Optional.<Srp> absent(),
                    createPath(Lists.newArrayList(DEFAULT_ENDPOINT_HOP))));
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

    private String getDestinationAddress(final List<Subobject> subobjects) {
        if (subobjects != null && !subobjects.isEmpty()) {
            final String prefix = ((IpPrefixCase) subobjects.get(subobjects.size() - 1).getSubobjectType())
                    .getIpPrefix().getIpPrefix().getIpv4Prefix().getValue();
            return prefix.substring(0, prefix.indexOf('/'));
        }
        return this.address;
    }

    private static Subobject getDefaultEROEndpointHop() {
        final SubobjectBuilder builder = new SubobjectBuilder();
        builder.setLoose(false);
        builder.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(new IpPrefixBuilder().setIpPrefix(
                new IpPrefix(new Ipv4Prefix(ENDPOINT_PREFIX))).build()).build());
        return builder.build();
    }

    private Random rnd = new Random();

    private PCEPErrors getRandomError() {
        return PCEPErrors.values()[rnd.nextInt(PCEPErrors.values().length)];
    }

}
