/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.List;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07PCReportMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs7;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LspId;

public class SrPcRptMessageParser extends Stateful07PCReportMessageParser {

    public SrPcRptMessageParser(ObjectRegistry registry) {
        super(registry);
    }

    @Override
    protected void serializeReport(Reports report, ByteBuf buffer) {
        if (report.getPath() != null && SrEroUtil.isSegmentRoutingPath(report.getPath().getEro())) {
            serializeObject(SrEroUtil.addSRPathSetupTypeTlv(report.getSrp()), buffer);
            serializeObject(report.getLsp(), buffer);
            serializeObject(report.getPath().getEro(), buffer);
        } else {
            super.serializeReport(report, buffer);
        }
    }

    @Override
    protected Reports getValidReports(List<Object> objects, List<Message> errors) {
        if (!(objects.get(0) instanceof Srp)) {
            errors.add(createErrorMsg(PCEPErrors.SRP_MISSING, Optional.<Rp>absent()));
        }
        final Srp srp = (Srp) objects.get(0);
        if (isSegmentRoutingPath(srp)) {
            boolean isValid = true;
            final ReportsBuilder builder = new ReportsBuilder();
            builder.setSrp(srp);
            objects.remove(0);
            if (objects.get(0) instanceof Lsp) {
                final Lsp lsp = (Lsp) objects.get(0);
                final LspBuilder lspBuilder = new LspBuilder(lsp);
                final TlvsBuilder tlvsBuilder = new TlvsBuilder(lsp.getTlvs());
                tlvsBuilder.setLspIdentifiers(new LspIdentifiersBuilder().setLspId(new LspId(lsp.getPlspId().getValue())).build());
                lspBuilder.setTlvs(tlvsBuilder.build());
                builder.setLsp(lspBuilder.build());
                objects.remove(0);
            } else {
                errors.add(createErrorMsg(PCEPErrors.LSP_MISSING, Optional.<Rp>absent()));
                isValid = false;
            }

            final Object obj = objects.get(0);
            if (obj instanceof Ero) {
                final Ero ero = (Ero) obj;
                final PCEPErrors error = SrEroUtil.validateSrEroSubobjects(ero);
                if (error != null) {
                    errors.add(createErrorMsg(error, Optional.<Rp>absent()));
                    isValid = false;
                } else {
                    builder.setPath(new PathBuilder().setEro(ero).build());
                }
                objects.remove(0);
            } else {
                errors.add(createErrorMsg(PCEPErrors.ERO_MISSING, Optional.<Rp>absent()));
                isValid = false;
            }
            if (isValid) {
                return builder.build();
            }
            return null;
        }
        return super.getValidReports(objects, errors);
    }

    private boolean isSegmentRoutingPath(final Srp srp) {
        if (srp != null && srp.getTlvs() != null) {
            return SrEroUtil.isPst(srp.getTlvs().getAugmentation(Tlvs7.class));
        }
        return false;
    }

}
