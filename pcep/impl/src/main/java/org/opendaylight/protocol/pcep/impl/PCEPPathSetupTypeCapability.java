/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev250930.PathSetupTypeCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.PsType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.setup.type.capability.tlv.PathSetupTypeCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.setup.type.capability.tlv.PathSetupTypeCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.sr.pce.capability.tlv.SrPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.srv6.pce.capability.tlv.Srv6PceCapabilityBuilder;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PCEPPathSetupTypeCapability extends PCEPCapability {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPPathSetupTypeCapability.class);
    private final PathSetupTypeCapability pstCapability;

    public PCEPPathSetupTypeCapability() {
        this(true, true, true);
    }

    public PCEPPathSetupTypeCapability(final boolean rsvpTe, final boolean srMpls, final boolean srv6) {
        final var psts = constructPstList(rsvpTe, srMpls, srv6);
        final var builder = new PathSetupTypeCapabilityBuilder().setPsts(psts);
        if (srMpls || srv6) {
            constructPstTlvs(psts, builder);
        }
        pstCapability = builder.build();
    }

    public PCEPPathSetupTypeCapability(final PathSetupTypeCapabilities config) {
        this(config.getRsvpTe(), config.getSrMpls(), config.getSrv6());
    }

    @Override
    public void setCapabilityProposal(final InetSocketAddress address, final TlvsBuilder builder) {
        builder.setPathSetupTypeCapability(pstCapability);
    }

    private static List<PsType> constructPstList(final boolean rsvpTe, final boolean srMpls,
            final boolean srv6) {
        final var list = new ArrayList<PsType>();
        if (rsvpTe) {
            list.add(PsType.RsvpTe);
        }
        if (srMpls) {
            list.add(PsType.SrMpls);
        }
        if (srv6) {
            list.add(PsType.Srv6);
        }
        LOG.info("Build Path Setup Type List: {}", list);
        return list;
    }

    private static void constructPstTlvs(final List<PsType> psts, PathSetupTypeCapabilityBuilder builder) {
        if (psts.contains(PsType.SrMpls)) {
            builder.setSrPceCapability(new SrPceCapabilityBuilder()
                .setMsd(Uint8.ZERO)
                .setNFlag(true)
                .setXFlag(true)
                .build());
        }
        if (psts.contains(PsType.Srv6)) {
            builder.setSrv6PceCapability(new Srv6PceCapabilityBuilder()
                .setNFlag(true)
                .build());
        }
    }

    public PathSetupTypeCapability getPSTCapability() {
        return pstCapability;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(pstCapability.getPsts().toArray());
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof PCEPPathSetupTypeCapability other
            && pstCapability.getPsts().equals(other.getPSTCapability().getPsts());
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        pstCapability.getPsts().forEach(pst -> helper.addValue(pst.getName()));
        return helper;
    }
}
