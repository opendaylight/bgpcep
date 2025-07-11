/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.path.binding.tlv.PathBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.open.object.Open;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;

@Singleton
@MetaInfServices
@Component(immediate = true)
public final class StatefulActivator implements PCEPExtensionProviderActivator {
    @Inject
    public StatefulActivator() {

    }

    @Override
    public List<Registration> start(final PCEPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>();

        final ObjectRegistry objReg = context.getObjectHandlerRegistry();
        regs.add(context.registerMessageParser(StatefulPCUpdateRequestMessageParser.TYPE,
            new StatefulPCUpdateRequestMessageParser(objReg)));
        regs.add(context.registerMessageSerializer(Pcupd.class, new StatefulPCUpdateRequestMessageParser(objReg)));
        regs.add(context.registerMessageParser(StatefulPCReportMessageParser.TYPE,
            new StatefulPCReportMessageParser(objReg)));
        regs.add(context.registerMessageSerializer(Pcrpt.class, new StatefulPCReportMessageParser(objReg)));
        regs.add(context.registerMessageParser(StatefulPCRequestMessageParser.TYPE,
                new StatefulPCRequestMessageParser(objReg)));
        regs.add(context.registerMessageSerializer(Pcreq.class, new StatefulPCRequestMessageParser(objReg)));
        regs.add(context.registerMessageParser(StatefulErrorMessageParser.TYPE,
            new StatefulErrorMessageParser(objReg)));
        regs.add(context.registerMessageSerializer(Pcerr.class, new StatefulErrorMessageParser(objReg)));

        final TlvRegistry tlvReg = context.getTlvHandlerRegistry();
        final VendorInformationTlvRegistry viTlvReg = context.getVendorInformationTlvRegistry();
        regs.add(context.registerObjectParser(new StatefulLspObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectSerializer(Lsp.class, new StatefulLspObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectParser(new StatefulSrpObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectSerializer(Srp.class, new StatefulSrpObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectParser(new StatefulOpenObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectSerializer(Open.class, new StatefulOpenObjectParser(tlvReg, viTlvReg)));

        regs.add(context.registerTlvParser(StatefulLSPIdentifierIpv4TlvParser.TYPE,
            new StatefulLSPIdentifierIpv4TlvParser()));
        regs.add(context.registerTlvParser(StatefulLSPIdentifierIpv6TlvParser.TYPE,
            new StatefulLSPIdentifierIpv6TlvParser()));
        regs.add(context.registerTlvSerializer(LspIdentifiers.class, new StatefulLSPIdentifierIpv4TlvParser()));
        regs.add(context.registerTlvParser(StatefulLspUpdateErrorTlvParser.TYPE,
            new StatefulLspUpdateErrorTlvParser()));
        regs.add(context.registerTlvSerializer(LspErrorCode.class, new StatefulLspUpdateErrorTlvParser()));
        regs.add(context.registerTlvParser(StatefulRSVPErrorSpecTlvParser.TYPE,
            new StatefulRSVPErrorSpecTlvParser()));
        regs.add(context.registerTlvSerializer(RsvpErrorSpec.class, new StatefulRSVPErrorSpecTlvParser()));
        regs.add(context.registerTlvParser(StatefulStatefulCapabilityTlvParser.TYPE,
            new StatefulStatefulCapabilityTlvParser()));
        regs.add(context.registerTlvSerializer(Stateful.class, new StatefulStatefulCapabilityTlvParser()));
        regs.add(context.registerTlvParser(StatefulLspSymbolicNameTlvParser.TYPE,
            new StatefulLspSymbolicNameTlvParser()));
        regs.add(context.registerTlvSerializer(SymbolicPathName.class, new StatefulLspSymbolicNameTlvParser()));
        regs.add(context.registerTlvParser(PathBindingTlvParser.TYPE, new PathBindingTlvParser()));
        regs.add(context.registerTlvSerializer(PathBinding.class, new PathBindingTlvParser()));
        return regs;
    }
}
