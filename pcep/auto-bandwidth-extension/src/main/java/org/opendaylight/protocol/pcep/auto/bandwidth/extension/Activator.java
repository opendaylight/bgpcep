/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.auto.bandwidth.extension;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev181109.bandwidth.usage.object.BandwidthUsage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Pcrpt;
import org.opendaylight.yangtools.concepts.Registration;

@MetaInfServices(value = PCEPExtensionProviderActivator.class)
public class Activator extends AbstractPCEPExtensionProviderActivator {
    private final int bandwidthUsageObjectType;

    public Activator() {
        this(5);
    }

    public Activator(final int bandwidthUsageObjectType) {
        this.bandwidthUsageObjectType = bandwidthUsageObjectType;
    }

    @Override
    protected List<Registration> startImpl(final PCEPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>();

        final BandwidthUsageObjectCodec bandwidthUsageObjectCodec =
                new BandwidthUsageObjectCodec(bandwidthUsageObjectType);
        regs.add(context.registerObjectParser(bandwidthUsageObjectCodec));
        regs.add(context.registerObjectSerializer(BandwidthUsage.class, bandwidthUsageObjectCodec));

        final PcRptMessageCodec pcRptMessageCodec = new PcRptMessageCodec(context.getObjectHandlerRegistry());
        regs.add(context.registerMessageParser(PcRptMessageCodec.TYPE, pcRptMessageCodec));
        regs.add(context.registerMessageSerializer(Pcrpt.class, pcRptMessageCodec));

        return regs;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("bandwithUsage", bandwidthUsageObjectType).toString();
    }
}
