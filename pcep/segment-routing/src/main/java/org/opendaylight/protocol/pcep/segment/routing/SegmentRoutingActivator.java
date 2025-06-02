/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.add.lsp.input.arguments.ero.subobject.subobject.type.SrEroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.add.lsp.input.arguments.ero.subobject.subobject.type.Srv6EroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.add.lsp.input.arguments.rro.subobject.subobject.type.SrRroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.add.lsp.input.arguments.rro.subobject.subobject.type.Srv6RroType;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;

@Singleton
@MetaInfServices
@Component(immediate = true)
public class SegmentRoutingActivator implements PCEPExtensionProviderActivator {

    @Inject
    public SegmentRoutingActivator() {
        // Hidden on purpose
    }

    @Override
    public List<Registration> start(final PCEPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>();

        // SR-ERO SubTLVs
        final SrEroSubobjectParser srEroSubobjectParser = new SrEroSubobjectParser();
        regs.add(context.registerEROSubobjectParser(SrEroSubobjectParser.TYPE, srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.network.topology.topology.node.path.computation.client.reported.lsp.path.ero
            .subobject.subobject.type.SrEroType.class,srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcinitiate.pcinitiate.message.requests.ero.subobject.subobject.type
            .SrEroType.class, srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcrep.pcrep.message.replies.result.success._case.success.paths.ero.subobject
            .subobject.type.SrEroType.class, srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcrpt.pcrpt.message.reports.path.ero.subobject.subobject.type
            .SrEroType.class, srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcupd.pcupd.message.updates.path.ero.subobject.subobject.type
            .SrEroType.class, srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.update.lsp.input.arguments.ero.subobject.subobject.type.SrEroType.class,
            srEroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(SrEroType.class, srEroSubobjectParser));

        // SR-RRO SubTLVs
        final SrRroSubobjectParser srRroSubobjectParser = new SrRroSubobjectParser();
        regs.add(context.registerRROSubobjectParser(SrRroSubobjectParser.TYPE, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.network.topology.topology.node.path.computation.client.reported.lsp.path.rro
            .subobject.subobject.type.SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcinitiate.pcinitiate.message.requests.rro.subobject.subobject.type
            .SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcrep.pcrep.message.replies.result.failure._case.rro.subobject.subobject
            .type.SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcreq.pcreq.message.requests.segment.computation.p2p.reported.route.rro
            .subobject.subobject.type.SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcrpt.pcrpt.message.reports.path.rro.subobject.subobject.type
            .SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcupd.pcupd.message.updates.path.rro.subobject.subobject.type
            .SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.update.lsp.input.arguments.rro.subobject.subobject.type.SrRroType.class,
            srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcrep.pcrep.message.replies.result.success._case.success.paths.rro.subobject
            .subobject.type.SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcreq.pcreq.message.requests.segment.computation.p2p.rro.subobject.subobject
            .type.SrRroType.class, srRroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(SrRroType.class, srRroSubobjectParser));

        // SRv6-ERO SubTLVs
        final Srv6EroSubobjectParser srv6EroSubobjectParser = new Srv6EroSubobjectParser();
        regs.add(context.registerEROSubobjectParser(Srv6EroSubobjectParser.TYPE, srv6EroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.network.topology.topology.node.path.computation.client.reported.lsp.path.ero
            .subobject.subobject.type.Srv6EroType.class,srv6EroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcinitiate.pcinitiate.message.requests.ero.subobject.subobject.type
            .SrEroType.class, srv6EroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcrep.pcrep.message.replies.result.success._case.success.paths.ero.subobject
            .subobject.type.Srv6EroType.class, srv6EroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcrpt.pcrpt.message.reports.path.ero.subobject.subobject.type
            .Srv6EroType.class, srv6EroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcupd.pcupd.message.updates.path.ero.subobject.subobject.type
            .Srv6EroType.class, srv6EroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.update.lsp.input.arguments.ero.subobject.subobject.type.Srv6EroType.class,
            srv6EroSubobjectParser));
        regs.add(context.registerEROSubobjectSerializer(Srv6EroType.class, srv6EroSubobjectParser));

        // SRv6-RRO SubTLVs
        final Srv6RroSubobjectParser srv6RroSubobjectParser = new Srv6RroSubobjectParser();
        regs.add(context.registerRROSubobjectParser(Srv6RroSubobjectParser.TYPE, srv6RroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.network.topology.topology.node.path.computation.client.reported.lsp.path.rro
            .subobject.subobject.type.Srv6RroType.class, srv6RroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcinitiate.pcinitiate.message.requests.rro.subobject.subobject.type
            .Srv6RroType.class, srv6RroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcrep.pcrep.message.replies.result.failure._case.rro.subobject.subobject
            .type.Srv6RroType.class, srv6RroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcreq.pcreq.message.requests.segment.computation.p2p.reported.route.rro
            .subobject.subobject.type.Srv6RroType.class, srv6RroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcrpt.pcrpt.message.reports.path.rro.subobject.subobject.type
            .Srv6RroType.class, srv6RroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcupd.pcupd.message.updates.path.rro.subobject.subobject.type
            .Srv6RroType.class, srv6RroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.update.lsp.input.arguments.rro.subobject.subobject.type.Srv6RroType.class,
            srv6RroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcrep.pcrep.message.replies.result.success._case.success.paths.rro.subobject
            .subobject.type.Srv6RroType.class, srv6RroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.segment.routing.rev250402.pcreq.pcreq.message.requests.segment.computation.p2p.rro.subobject.subobject
            .type.Srv6RroType.class, srv6RroSubobjectParser));
        regs.add(context.registerRROSubobjectSerializer(Srv6RroType.class, srv6RroSubobjectParser));

        return regs;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).toString();
    }
}
