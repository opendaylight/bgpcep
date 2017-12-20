/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.auto.bandwidth.extension;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import io.netty.buffer.ByteBuf;
import java.util.List;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07PCReportMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev171025.Bandwidth1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev171025.Bandwidth1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev171025.bandwidth.usage.object.BandwidthUsage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev171025.bandwidth.usage.object.BandwidthUsageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.BandwidthBuilder;

public class PcRptMessageCodec extends Stateful07PCReportMessageParser {

    public PcRptMessageCodec(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    protected Reports getValidReports(final List<Object> objects, final List<Message> errors) {
        final Optional<Object> find = Iterables.tryFind(objects, Predicates.instanceOf(BandwidthUsage.class));
        final Object object;
        if (find.isPresent()) {
            object = find.get();
            objects.remove(object);
        } else {
            object = null;
        }
        final Reports validReports = super.getValidReports(objects, errors);
        if (object != null && validReports != null) {
            final Path path = validReports.getPath();
            if (path != null) {
                return new ReportsBuilder(validReports).setPath(new PathBuilder(path).setBandwidth(
                        setBandwidthUsage(path.getBandwidth(), (BandwidthUsage) object)).build()).build();
            }
        }
        return validReports;
    }

    @Override
    protected void serializeObject(final Object object, final ByteBuf buffer) {
        super.serializeObject(object, buffer);
        if (object instanceof Bandwidth) {
            final Bandwidth1 bw = ((Bandwidth) object).getAugmentation(Bandwidth1.class);
            if (bw != null) {
                super.serializeObject(new BandwidthUsageBuilder().setBwSample(bw.getBwSample()).build(), buffer);
            }

        }
    }

    private static Bandwidth setBandwidthUsage(final Bandwidth bandwidth, final BandwidthUsage bwUsage) {
        final BandwidthBuilder bandwidthBuilder;
        if (bandwidth != null) {
            bandwidthBuilder = new BandwidthBuilder(bandwidth);
        } else {
            bandwidthBuilder = new BandwidthBuilder();
        }
        bandwidthBuilder.addAugmentation(Bandwidth1.class, new Bandwidth1Builder()
                .setBwSample(bwUsage.getBwSample()).build()).build();
        return bandwidthBuilder.build();
    }

}
