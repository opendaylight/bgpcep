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
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07PCReportMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs7;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;

public class SrPcRptMessageParser extends Stateful07PCReportMessageParser {

    public SrPcRptMessageParser(ObjectRegistry registry) {
        super(registry);
    }

    @Override
    protected void serializeReport(Reports report, ByteBuf buffer) {
        if(isSegmentRoutingPath(report.getSrp())) {
            serializeObject(report.getSrp(), buffer);
            serializeObject(report.getLsp(), buffer);
            final Ero srEro = report.getPath().getEro();
            if(srEro != null) {
                serializeObject(srEro, buffer);
            }
        } else {
            super.serializeReport(report, buffer);
        }
    }

    @Override
    protected Reports getValidReports(List<Object> objects, List<Message> errors) {
        if(!(objects.get(0) instanceof Srp)) {
            errors.add(createErrorMsg(PCEPErrors.SRP_MISSING));
        }
        final Srp srp = (Srp) objects.get(0);
        if(isSegmentRoutingPath(srp)) {
            boolean isValid = true;
            final ReportsBuilder builder = new ReportsBuilder();
            builder.setSrp(srp);
            objects.remove(0);
            if (objects.get(0) instanceof Lsp) {
                builder.setLsp((Lsp) objects.get(0));
                objects.remove(0);
            } else {
                errors.add(createErrorMsg(PCEPErrors.LSP_MISSING));
                isValid = false;
            }

            final Object obj = objects.get(0);
            if(obj instanceof Ero) {
                final Ero ero = (Ero) obj;
                final PCEPErrors error = SrEroUtil.validateSrEroSubobjects(ero);
                if(error != null) {
                    errors.add(createErrorMsg(error));
                    isValid = false;
                } else {
                    builder.setPath(new PathBuilder().setEro(ero).build());
                }
                objects.remove(0);
            } else {
                errors.add(createErrorMsg(PCEPErrors.ERO_MISSING));
                isValid = false;
            }
            if(isValid) {
                return builder.build();
            }
            return null;
        }
        return super.getValidReports(objects, errors);
    }

    private boolean isSegmentRoutingPath(final Srp srp) {
        if(srp != null && srp.getTlvs() != null) {
            return SrEroUtil.isPst(srp.getTlvs().getAugmentation(Tlvs7.class));
        }
        return false;
    }

}
