/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bgp.linkstate.attribute;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.TeLspRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.TeLspAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.TeLspAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.te.lsp.attributes._case.TeLspAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.TeLspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.admin.status.object.AdminStatusObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.association.object.AssociationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.bandwidth.object.BandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.detour.object.DetourObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.object.ExcludeRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.explicit.route.object.ExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.fast.reroute.object.FastRerouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.flow.spec.object.FlowSpecObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.attributes.object.LspAttributesObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.required.attributes.object.LspRequiredAttributesObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.metric.object.MetricObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.primary.path.route.object.PrimaryPathRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.protection.object.ProtectionObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.record.route.object.RecordRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.secondary.explicit.route.object.SecondaryExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.secondary.record.route.object.SecondaryRecordRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.sender.tspec.object.SenderTspecObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.session.attribute.object.SessionAttributeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
final class TeLspAttributesParser {

    private static final Logger LOG = LoggerFactory.getLogger(TeLspAttributesParser.class);
    private static TeLspRegistry registry;

    public TeLspAttributesParser(final TeLspRegistry registry) {
        TeLspAttributesParser.registry = registry;
    }

    public static LinkStateAttribute parseTeLspAttributes(final ByteBuf attributes) throws BGPParsingException {

        TeLspAttributesBuilder builder = new TeLspAttributesBuilder();
        LOG.trace("Initiated parsing TE LSP Objects.");
        while (attributes.isReadable()) {
            final int length = attributes.readUnsignedShort();
            final int classNum = attributes.readUnsignedByte();
            final int cType = attributes.readUnsignedByte();
            final ByteBuf value = attributes.readSlice(length);
            addObject(builder, registry.parseTeLsp(classNum, cType, value));
        }
        LOG.trace("Finished parsing TE LSP Objects.");
        return new TeLspAttributesCaseBuilder().setTeLspAttributes(builder.build()).build();
    }

    private static void addObject(final TeLspAttributesBuilder builder, final TeLspObject object) {
        if (object instanceof SenderTspecObject) {
            builder.setSenderTspecObject((SenderTspecObject) object);
        } else if (object instanceof FlowSpecObject) {
            builder.setFlowSpecObject((FlowSpecObject) object);
        } else if (object instanceof SessionAttributeObject) {
            builder.setSessionAttributeObject((SessionAttributeObject) object);
        } else if (object instanceof ExplicitRouteObject) {
            builder.setExplicitRouteObject((ExplicitRouteObject) object);
        } else if (object instanceof RecordRouteObject) {
            builder.setRecordRouteObject((RecordRouteObject) object);
        } else if (object instanceof FastRerouteObject) {
            builder.setFastRerouteObject((FastRerouteObject) object);
        } else if (object instanceof DetourObject) {
            builder.setDetourObject((DetourObject) object);
        } else if (object instanceof ExcludeRouteObject) {
            builder.setExcludeRouteObject((ExcludeRouteObject) object);
        } else if (object instanceof SecondaryExplicitRouteObject) {
            builder.setSecondaryExplicitRouteObject((SecondaryExplicitRouteObject) object);
        } else if (object instanceof SecondaryRecordRouteObject) {
            builder.setSecondaryRecordRouteObject((SecondaryRecordRouteObject) object);
        } else if (object instanceof LspAttributesObject) {
            builder.setLspAttributesObject((LspAttributesObject) object);
        } else if (object instanceof LspRequiredAttributesObject) {
            builder.setLspRequiredAttributesObject((LspRequiredAttributesObject) object);
        } else if (object instanceof ProtectionObject) {
            builder.setProtectionObject((ProtectionObject) object);
        } else if (object instanceof AssociationObject) {
            builder.setAssociationObject((AssociationObject) object);
        } else if (object instanceof PrimaryPathRouteObject) {
            builder.setPrimaryPathRouteObject((PrimaryPathRouteObject) object);
        } else if (object instanceof AdminStatusObject) {
            builder.setAdminStatusObject((AdminStatusObject) object);
        } else if (object instanceof BandwidthObject) {
            builder.setBandwidthObject((BandwidthObject) object);
        } else if (object instanceof Metric) {
            builder.setMetricObject((MetricObject) object);
        } else {
            throw new IllegalStateException("Unhandled TE LSP Object " + object);
        }
    }


    public static void serializeLspAttributes(final TeLspAttributesCase linkState, final ByteBuf output) {
        LOG.trace("Started serializing TE LSP Objects");
        final ByteBuf byteBuf = Unpooled.buffer();

        final SenderTspecObject tSpec = linkState.getTeLspAttributes().getSenderTspecObject();
        if (tSpec != null) {
            registry.serializeTeLsp(tSpec, tSpec.getCType(), byteBuf);
        }

        final FlowSpecObject flow = linkState.getTeLspAttributes().getFlowSpecObject();
        if (flow != null) {
            registry.serializeTeLsp(flow, flow.getCType(), byteBuf);
        }

        final SessionAttributeObject sao = linkState.getTeLspAttributes().getSessionAttributeObject();
        if (sao != null) {
            registry.serializeTeLsp(sao, sao.getCType(), byteBuf);
        }

        final ExplicitRouteObject ero = linkState.getTeLspAttributes().getExplicitRouteObject();
        if (ero != null) {
            registry.serializeTeLsp(ero, ero.getCType(), byteBuf);
        }

        final RecordRouteObject rro = linkState.getTeLspAttributes().getRecordRouteObject();
        if (rro != null) {
            registry.serializeTeLsp(rro, rro.getCType(), byteBuf);
        }

        final FastRerouteObject fro = linkState.getTeLspAttributes().getFastRerouteObject();
        if (fro != null) {
            registry.serializeTeLsp(fro, fro.getCType(), byteBuf);
        }

        final DetourObject dto = linkState.getTeLspAttributes().getDetourObject();
        if (dto != null) {
            registry.serializeTeLsp(dto, dto.getCType(), byteBuf);
        }

        final ExcludeRouteObject exro = linkState.getTeLspAttributes().getExcludeRouteObject();
        if (exro != null) {
            registry.serializeTeLsp(exro, exro.getCType(), byteBuf);
        }

        final SecondaryExplicitRouteObject sero = linkState.getTeLspAttributes().getSecondaryExplicitRouteObject();
        if (sero != null) {
            registry.serializeTeLsp(sero, sero.getCType(), byteBuf);
        }

        final SecondaryRecordRouteObject srro = linkState.getTeLspAttributes().getSecondaryRecordRouteObject();
        if (srro != null) {
            registry.serializeTeLsp(srro, srro.getCType(), byteBuf);
        }

        final LspAttributesObject lspAtt = linkState.getTeLspAttributes().getLspAttributesObject();
        if (lspAtt != null) {
            registry.serializeTeLsp(lspAtt, lspAtt.getCType(), byteBuf);
        }

        final LspRequiredAttributesObject rao = linkState.getTeLspAttributes().getLspRequiredAttributesObject();
        if (rao != null) {
            registry.serializeTeLsp(rao, rao.getCType(), byteBuf);
        }

        final ProtectionObject po = linkState.getTeLspAttributes().getProtectionObject();
        if (po != null) {
            registry.serializeTeLsp(po, po.getCType(), byteBuf);
        }

        final AssociationObject aso = linkState.getTeLspAttributes().getAssociationObject();
        if (aso != null) {
            registry.serializeTeLsp(aso, aso.getCType(), byteBuf);
        }

        final PrimaryPathRouteObject ppr = linkState.getTeLspAttributes().getPrimaryPathRouteObject();
        if (ppr != null) {
            registry.serializeTeLsp(ppr, ppr.getCType(), byteBuf);
        }

        final AdminStatusObject adso = linkState.getTeLspAttributes().getAdminStatusObject();
        if (adso != null) {
            registry.serializeTeLsp(adso, adso.getCType(), byteBuf);
        }

        final BandwidthObject bo = linkState.getTeLspAttributes().getBandwidthObject();
        if (bo != null) {
            registry.serializeTeLsp(bo, bo.getCType(), byteBuf);
        }

        final MetricObject mo = linkState.getTeLspAttributes().getMetricObject();
        if (mo != null) {
            registry.serializeTeLsp(mo, mo.getCType(), byteBuf);
        }

        output.writeShort(99);
        output.writeShort(byteBuf.readableBytes());
        output.writeBytes(byteBuf);
        LOG.trace("Finished serializing TE LSP Objects");
    }
}
