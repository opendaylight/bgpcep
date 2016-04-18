/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.CRouterIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;

@VisibleForTesting
public final class PrefixIpv6NlriParser implements NlriTypeCaseParser, NodeDescriptorsTlvBuilderParser {

    /* Prefix Descriptor QNames */
    @VisibleForTesting
    public static final NodeIdentifier OSPF_ROUTE_NID = new NodeIdentifier(QName.create(PrefixDescriptors.QNAME, "ospf-route-type").intern());
    @VisibleForTesting
    public static final NodeIdentifier IP_REACH_NID = new NodeIdentifier(QName.create(PrefixDescriptors.QNAME, "ip-reachability-information").intern());

    @Override
    public void setAsNumBuilder(AsNumber asNum, NlriTlvTypeBuilderContext context) {
        context.getAdvertisingNodeDescriptorsBuilder().setAsNumber(asNum);
    }

    @Override
    public void setAreaIdBuilder(AreaIdentifier ai, NlriTlvTypeBuilderContext context) {
        context.getAdvertisingNodeDescriptorsBuilder().setAreaId(ai);
    }

    @Override
    public void setCRouterIdBuilder(CRouterIdentifier CRouterId, NlriTlvTypeBuilderContext context) {
        context.getAdvertisingNodeDescriptorsBuilder().setCRouterIdentifier(CRouterId);
    }

    @Override
    public void setDomainIdBuilder(DomainIdentifier bgpId, NlriTlvTypeBuilderContext context) {
        context.getAdvertisingNodeDescriptorsBuilder().setDomainId(bgpId);
    }

    @Override
    public org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier buildNodeDescriptors(NlriTlvTypeBuilderContext context) {
        return context.getAdvertisingNodeDescriptorsBuilder().build();
    }

    @Override
    public ObjectType parseTypeNlri(final ByteBuf nlri, final NlriType type, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier localdescriptor, final ByteBuf restNlri) throws BGPParsingException {
        final PrefixCaseBuilder prefixbuilder = new PrefixCaseBuilder();
        final SimpleNlriTypeRegistry nlriTypeReg = SimpleNlriTypeRegistry.getInstance();
        final NlriTlvTypeBuilderContext context = new NlriTlvTypeBuilderContext();
        nlriTypeReg.parseTlvObject(restNlri, type, context);
        PrefixDescriptors prefdesc = context.getPrefixDescriptorsBuilder().build();
        PrefixCase prefixcase = prefixbuilder.setAdvertisingNodeDescriptors((AdvertisingNodeDescriptors) localdescriptor).setPrefixDescriptors(prefdesc).build();
        return prefixcase;
    }

}
