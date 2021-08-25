/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.auto.bandwidth.extension;

import com.google.common.base.MoreObjects;
import java.util.List;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev181109.bandwidth.usage.object.BandwidthUsage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev210825.Pcrpt;
import org.opendaylight.yangtools.concepts.Registration;

@MetaInfServices
public class Activator implements PCEPExtensionProviderActivator {
    private final int bandwidthUsageObjectType;

    public Activator() {
        this(5);
    }

    public Activator(final int bandwidthUsageObjectType) {
        this.bandwidthUsageObjectType = bandwidthUsageObjectType;
    }

    @Override
    public List<Registration> start(final PCEPExtensionProviderContext context) {
        final BandwidthUsageObjectCodec bandwidthUsageObjectCodec =
                new BandwidthUsageObjectCodec(bandwidthUsageObjectType);
        final PcRptMessageCodec pcRptMessageCodec = new PcRptMessageCodec(context.getObjectHandlerRegistry());

        return List.of(
            context.registerObjectParser(bandwidthUsageObjectCodec),
            context.registerObjectSerializer(BandwidthUsage.class, bandwidthUsageObjectCodec),
            context.registerMessageParser(PcRptMessageCodec.TYPE, pcRptMessageCodec),
            context.registerMessageSerializer(Pcrpt.class, pcRptMessageCodec));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("bandwithUsage", bandwidthUsageObjectType).toString();
    }
}
