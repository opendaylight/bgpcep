/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.tlvs;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import org.opendaylight.protocol.bgp.linkstate.nlri.SimpleNlriTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.CRouterIdentifier;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.common.QName;

abstract class AbstractNodeDescriptorTlvCodec {

    protected final SimpleNlriTypeRegistry tlvReg;

    AbstractNodeDescriptorTlvCodec(final SimpleNlriTypeRegistry tlvReg) {
        this.tlvReg = tlvReg;
    }

    public final NodeIdentifier parseNodeDescriptor(final ByteBuf value, final Map<QName, Object> parsedSubTlvs) {
        parsedSubTlvs.putAll(this.tlvReg.parseSubTlvs(value));
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

    public final void serializeNodeDescriptor(final NodeIdentifier tlv, final ByteBuf body) {
        this.tlvReg.serializeTlv(AsNumTlvParser.AS_NUMBER_QNAME, tlv.getAsNumber(), body);
        this.tlvReg.serializeTlv(DomainIdTlvParser.DOMAIN_ID_QNAME, tlv.getDomainId(), body);
        this.tlvReg.serializeTlv(AreaIdTlvParser.AREA_ID_QNAME, tlv.getAreaId(), body);
        this.tlvReg.serializeTlv(CRouterIdentifier.QNAME, tlv.getCRouterIdentifier(), body);
    }

}
