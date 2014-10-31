/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import io.netty.buffer.ByteBuf;
import java.util.List;
import org.opendaylight.protocol.pcep.ietf.initiated00.CInitiated00PCInitiateMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs7;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;

public class SrPcInitiateMessageParser extends CInitiated00PCInitiateMessageParser {

    public SrPcInitiateMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    protected void serializeRequest(final Requests req, final ByteBuf buffer) {
        if (SrEroUtil.isSegmentRoutingPath(req.getEro())) {
            serializeObject(SrEroUtil.addSRPathSetupTypeTlv(req.getSrp()), buffer);
            serializeObject(req.getLsp(), buffer);
            serializeObject(req.getEro(), buffer);
        } else {
            super.serializeRequest(req, buffer);
        }
    }

    @Override
    protected Requests getValidRequest(final List<Object> objects) {
        final Srp srp = (Srp) objects.get(0);
        if (isSegmentRoutingPath(srp)) {
            final RequestsBuilder builder = new RequestsBuilder();
            builder.setSrp(srp);
            objects.remove(0);
            builder.setLsp((Lsp) objects.get(0));
            objects.remove(0);

            final Object obj = objects.get(0);
            if (obj instanceof Ero) {
                builder.setEro((Ero) obj);
                objects.remove(0);
            }
            return builder.build();
        }
        return super.getValidRequest(objects);
    }

    private boolean isSegmentRoutingPath(final Srp srp) {
        if (srp != null && srp.getTlvs() != null) {
            return SrEroUtil.isPst(srp.getTlvs().getAugmentation(Tlvs5.class))
                    || SrEroUtil.isPst(srp.getTlvs().getAugmentation(Tlvs7.class));
        }
        return false;
    }

}
