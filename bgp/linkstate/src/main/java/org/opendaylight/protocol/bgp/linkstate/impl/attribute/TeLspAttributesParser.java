/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.impl.attribute;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.TeLspAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.TeLspAttributesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.te.lsp.attributes._case.TeLspAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.te.lsp.attributes._case.TeLspAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.admin.status.object.AdminStatusObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.association.object.AssociationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.bandwidth.object.BandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.bandwidth.object.bandwidth.object.BasicBandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.bandwidth.object.bandwidth.object.ReoptimizationBandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.DetourObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.Ipv4DetourObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.Ipv6DetourObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.ExcludeRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.object.ExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.fast.reroute.object.FastRerouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.fast.reroute.object.fast.reroute.object.BasicFastRerouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.fast.reroute.object.fast.reroute.object.LegacyFastRerouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.flow.spec.object.FlowSpecObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.attributes.object.LspAttributesObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.required.attributes.object.LspRequiredAttributesObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.metric.object.MetricObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.primary.path.route.object.PrimaryPathRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.object.ProtectionObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.object.protection.object.BasicProtectionObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.object.protection.object.DynamicControlProtectionObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.object.RecordRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.explicit.route.object.SecondaryExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.record.route.object.SecondaryRecordRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.session.attribute.object.SessionAttributeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.session.attribute.object.session.attribute.object.BasicSessionAttributeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.session.attribute.object.session.attribute.object.SessionAttributeObjectWithResourcesAffinities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.tspec.object.TspecObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TeLspAttributesParser {

    private static final Logger LOG = LoggerFactory.getLogger(TeLspAttributesParser.class);
    //TBD BY IANA
    private static final int MAGIC_NUMBER = 99;

    private TeLspAttributesParser() {
        throw new UnsupportedOperationException();
    }

    static LinkStateAttribute parseTeLspAttributes(final RSVPTeObjectRegistry registry, final ByteBuf attributes) throws BGPParsingException {

        final TeLspAttributesBuilder builder = new TeLspAttributesBuilder();
        LOG.trace("Initiated parsing TE LSP Objects.");
        while (attributes.isReadable()) {
            final int length = attributes.readUnsignedShort();
            final int classNum = attributes.readUnsignedByte();
            final int cType = attributes.readUnsignedByte();
            final ByteBuf value = attributes.readSlice(length);
            try {
                addObject(builder, registry.parseRSPVTe(classNum, cType, value));
            } catch (final RSVPParsingException e) {
                LOG.debug("Parsering TE LSP Object error. class number: {} cType: {} value: {}", classNum, cType, value, e);
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


    static void serializeLspAttributes(final RSVPTeObjectRegistry registry, final TeLspAttributesCase linkState, final ByteBuf output) {
        LOG.trace("Started serializing TE LSP Objects");
        final ByteBuf byteBuf = Unpooled.buffer();

        final TeLspAttributes teLspAttribute = linkState.getTeLspAttributes();
        final TspecObject tSpec = teLspAttribute.getTspecObject();
        registry.serializeRSPVTe(tSpec, byteBuf);

        final FlowSpecObject flow = teLspAttribute.getFlowSpecObject();
        registry.serializeRSPVTe(flow, byteBuf);

        final SessionAttributeObject sao = teLspAttribute.getSessionAttributeObject();
        if (sao instanceof BasicSessionAttributeObject) {
            registry.serializeRSPVTe((BasicSessionAttributeObject) sao, byteBuf);
        } else if (sao instanceof SessionAttributeObjectWithResourcesAffinities) {
            registry.serializeRSPVTe((SessionAttributeObjectWithResourcesAffinities) sao, byteBuf);
        }

        final ExplicitRouteObject ero = teLspAttribute.getExplicitRouteObject();
        registry.serializeRSPVTe(ero, byteBuf);

        final RecordRouteObject rro = teLspAttribute.getRecordRouteObject();
        registry.serializeRSPVTe(rro, byteBuf);

        final FastRerouteObject fro = teLspAttribute.getFastRerouteObject();
        if (fro instanceof BasicFastRerouteObject) {
            registry.serializeRSPVTe((BasicFastRerouteObject) fro, byteBuf);
        } else if (fro instanceof LegacyFastRerouteObject) {
            registry.serializeRSPVTe((LegacyFastRerouteObject) fro, byteBuf);
        }

        final DetourObject dto = teLspAttribute.getDetourObject();
        if (dto instanceof Ipv4DetourObject) {
            registry.serializeRSPVTe((Ipv4DetourObject) dto, byteBuf);
        } else if (dto instanceof Ipv6DetourObject) {
            registry.serializeRSPVTe((Ipv6DetourObject) dto, byteBuf);
        }

        final ExcludeRouteObject exro = teLspAttribute.getExcludeRouteObject();
        registry.serializeRSPVTe(exro, byteBuf);

        final SecondaryExplicitRouteObject sero = teLspAttribute.getSecondaryExplicitRouteObject();
        registry.serializeRSPVTe(sero, byteBuf);

        final SecondaryRecordRouteObject srro = teLspAttribute.getSecondaryRecordRouteObject();
        registry.serializeRSPVTe(srro, byteBuf);

        final LspAttributesObject lspAtt = teLspAttribute.getLspAttributesObject();
        registry.serializeRSPVTe(lspAtt, byteBuf);

        final LspRequiredAttributesObject rao = teLspAttribute.getLspRequiredAttributesObject();
        registry.serializeRSPVTe(rao, byteBuf);

        final ProtectionObject po = teLspAttribute.getProtectionObject();
        if (po instanceof DynamicControlProtectionObject) {
            registry.serializeRSPVTe((DynamicControlProtectionObject) po, byteBuf);
        } else if (po instanceof BasicProtectionObject) {
            registry.serializeRSPVTe((BasicProtectionObject) po, byteBuf);
        }

        final AssociationObject aso = teLspAttribute.getAssociationObject();
        registry.serializeRSPVTe(aso, byteBuf);

        final PrimaryPathRouteObject ppr = teLspAttribute.getPrimaryPathRouteObject();
        registry.serializeRSPVTe(ppr, byteBuf);

        final AdminStatusObject adso = teLspAttribute.getAdminStatusObject();
        registry.serializeRSPVTe(adso, byteBuf);

        final BandwidthObject bo = teLspAttribute.getBandwidthObject();
        if (bo instanceof BasicBandwidthObject) {
            registry.serializeRSPVTe((BasicBandwidthObject) bo, byteBuf);
        } else if (bo instanceof ReoptimizationBandwidthObject) {
            registry.serializeRSPVTe((ReoptimizationBandwidthObject) bo, byteBuf);
        }

        final MetricObject mo = teLspAttribute.getMetricObject();
        registry.serializeRSPVTe(mo, byteBuf);


        output.writeShort(MAGIC_NUMBER);
        output.writeShort(byteBuf.readableBytes());
        output.writeBytes(byteBuf);
        LOG.trace("Finished serializing TE LSP Objects");
    }
}
