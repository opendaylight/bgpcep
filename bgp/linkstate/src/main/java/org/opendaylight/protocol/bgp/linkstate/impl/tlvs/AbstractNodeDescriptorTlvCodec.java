/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.CRouterIdentifier;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.common.QName;

abstract class AbstractNodeDescriptorTlvCodec {
    protected static NodeIdentifier parseNodeDescriptor(final ByteBuf value, final Map<QName, Object> parsedSubTlvs) {
        parsedSubTlvs.putAll(SimpleNlriTypeRegistry.getInstance().parseSubTlvs(value));
        final AsNumber asNumber = (AsNumber) parsedSubTlvs.get(AsNumTlvParser.AS_NUMBER_QNAME);
        final DomainIdentifier domainId = (DomainIdentifier) parsedSubTlvs.get(DomainIdTlvParser.DOMAIN_ID_QNAME);
        final AreaIdentifier areaId = (AreaIdentifier) parsedSubTlvs.get(AreaIdTlvParser.AREA_ID_QNAME);
        final CRouterIdentifier routerId = (CRouterIdentifier) parsedSubTlvs.get(CRouterIdentifier.QNAME);

        return new NodeIdentifier() {
            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return NodeIdentifier.class;
            }
            @Override
            public AsNumber getAsNumber() {
                return asNumber;
            }
            @Override
            public DomainIdentifier getDomainId() {
                return domainId;
            }
            @Override
            public AreaIdentifier getAreaId() {
                return areaId;
            }
            @Override
            public CRouterIdentifier getCRouterIdentifier() {
                return routerId;
            }
        };
    }

    protected static void serializeNodeDescriptor(final NodeIdentifier tlv, final ByteBuf body) {
        final SimpleNlriTypeRegistry tlvReg = SimpleNlriTypeRegistry.getInstance();
        tlvReg.serializeTlv(AsNumTlvParser.AS_NUMBER_QNAME, tlv.getAsNumber(), body);
        tlvReg.serializeTlv(DomainIdTlvParser.DOMAIN_ID_QNAME, tlv.getDomainId(), body);
        tlvReg.serializeTlv(AreaIdTlvParser.AREA_ID_QNAME, tlv.getAreaId(), body);
        tlvReg.serializeTlv(CRouterIdentifier.QNAME, tlv.getCRouterIdentifier(), body);
    }

}
