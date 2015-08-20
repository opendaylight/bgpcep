/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.attribute;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.TeLspAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.TeLspAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.te.lsp.attributes._case.TeLspAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.admin.status.object.AdminStatusObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.association.object.AssociationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.bandwidth.object.BandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.DetourObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.ExcludeRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.object.ExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.fast.reroute.object.FastRerouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.flow.spec.object.FlowSpecObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.attributes.object.LspAttributesObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.required.attributes.object.LspRequiredAttributesObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.metric.object.MetricObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.primary.path.route.object.PrimaryPathRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.object.ProtectionObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.object.RecordRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.explicit.route.object.SecondaryExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.record.route.object.SecondaryRecordRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.session.attribute.object.SessionAttributeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.tspec.object.TspecObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.spi.RSVPParsingException;
import parser.spi.RSVPTeObjectRegistry;

@VisibleForTesting
final class TeLspAttributesParser {

    private static final Logger LOG = LoggerFactory.getLogger(TeLspAttributesParser.class);
    private static RSVPTeObjectRegistry registry;


    public TeLspAttributesParser(final RSVPTeObjectRegistry registry) {
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
            try {
                addObject(builder, registry.parseRSPVTe(classNum, cType, value));
            } catch (RSVPParsingException e) {
                throw new BGPParsingException(e.getMessage());
            }
        }
        LOG.trace("Finished parsing TE LSP Objects.");
        return new TeLspAttributesCaseBuilder().setTeLspAttributes(builder.build()).build();
    }

    private static void addObject(final TeLspAttributesBuilder builder, final RsvpTeObject object) {
        if (object instanceof TspecObject) {
            builder.setTspecObject((TspecObject) object);
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
        } else if (object instanceof MetricObject) {
            builder.setMetricObject((MetricObject) object);
        } else {
            throw new IllegalStateException("Unhandled TE LSP Object " + object);
        }
    }


    public static void serializeLspAttributes(final TeLspAttributesCase linkState, final ByteBuf output) {
        LOG.trace("Started serializing TE LSP Objects");
        final ByteBuf byteBuf = Unpooled.buffer();

        final TspecObject tSpec = linkState.getTeLspAttributes().getTspecObject();
        if (tSpec != null) {
            registry.serializeRSPVTe(tSpec, tSpec.getCType(), byteBuf);
        }

        final FlowSpecObject flow = linkState.getTeLspAttributes().getFlowSpecObject();
        if (flow != null) {
            registry.serializeRSPVTe(flow, flow.getCType(), byteBuf);
        }

        final SessionAttributeObject sao = linkState.getTeLspAttributes().getSessionAttributeObject();
        if (sao != null) {
            registry.serializeRSPVTe(sao, sao.getCType(), byteBuf);
        }

        final ExplicitRouteObject ero = linkState.getTeLspAttributes().getExplicitRouteObject();
        if (ero != null) {
            registry.serializeRSPVTe(ero, ero.getCType(), byteBuf);
        }

        final RecordRouteObject rro = linkState.getTeLspAttributes().getRecordRouteObject();
        if (rro != null) {
            registry.serializeRSPVTe(rro, rro.getCType(), byteBuf);
        }

        final FastRerouteObject fro = linkState.getTeLspAttributes().getFastRerouteObject();
        if (fro != null) {
            registry.serializeRSPVTe(fro, fro.getCType(), byteBuf);
        }

        final DetourObject dto = linkState.getTeLspAttributes().getDetourObject();
        if (dto != null) {
            registry.serializeRSPVTe(dto, dto.getCType(), byteBuf);
        }

        final ExcludeRouteObject exro = linkState.getTeLspAttributes().getExcludeRouteObject();
        if (exro != null) {
            registry.serializeRSPVTe(exro, exro.getCType(), byteBuf);
        }

        final SecondaryExplicitRouteObject sero = linkState.getTeLspAttributes().getSecondaryExplicitRouteObject();
        if (sero != null) {
            registry.serializeRSPVTe(sero, sero.getCType(), byteBuf);
        }

        final SecondaryRecordRouteObject srro = linkState.getTeLspAttributes().getSecondaryRecordRouteObject();
        if (srro != null) {
            registry.serializeRSPVTe(srro, srro.getCType(), byteBuf);
        }

        final LspAttributesObject lspAtt = linkState.getTeLspAttributes().getLspAttributesObject();
        if (lspAtt != null) {
            registry.serializeRSPVTe(lspAtt, lspAtt.getCType(), byteBuf);
        }

        final LspRequiredAttributesObject rao = linkState.getTeLspAttributes().getLspRequiredAttributesObject();
        if (rao != null) {
            registry.serializeRSPVTe(rao, rao.getCType(), byteBuf);
        }

        final ProtectionObject po = linkState.getTeLspAttributes().getProtectionObject();
        if (po != null) {
            registry.serializeRSPVTe(po, po.getCType(), byteBuf);
        }

        final AssociationObject aso = linkState.getTeLspAttributes().getAssociationObject();
        if (aso != null) {
            registry.serializeRSPVTe(aso, aso.getCType(), byteBuf);
        }

        final PrimaryPathRouteObject ppr = linkState.getTeLspAttributes().getPrimaryPathRouteObject();
        if (ppr != null) {
            registry.serializeRSPVTe(ppr, ppr.getCType(), byteBuf);
        }

        final AdminStatusObject adso = linkState.getTeLspAttributes().getAdminStatusObject();
        if (adso != null) {
            registry.serializeRSPVTe(adso, adso.getCType(), byteBuf);
        }

        final BandwidthObject bo = linkState.getTeLspAttributes().getBandwidthObject();
        if (bo != null) {
            registry.serializeRSPVTe(bo, bo.getCType(), byteBuf);
        }

        final MetricObject mo = linkState.getTeLspAttributes().getMetricObject();
        if (mo != null) {
            registry.serializeRSPVTe(mo, mo.getCType(), byteBuf);
        }

        output.writeShort(99);
        output.writeShort(byteBuf.readableBytes());
        output.writeBytes(byteBuf);
        LOG.trace("Finished serializing TE LSP Objects");
    }
}
