/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object.end.points;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.P2mpIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.P2mpIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.ipv4._case.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.ipv6._case.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.p2mp.ipv4._case.P2mpIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.p2mp.ipv6._case.P2mpIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObj;

public final class PCEPEndPointsObjectSerializer implements ObjectSerializer {

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof EndpointsObj,
            "Wrong instance of PCEPObject. Passed %s. Needed EndpointsObject.", object.getClass());
        final EndpointsObj ePObj = (EndpointsObj) object;
        final AddressFamily afi = ePObj.getAddressFamily();
        final Boolean processing = object.isProcessingRule();
        final Boolean ignore = object.isIgnore();
        if (afi instanceof Ipv6Case) {
            final Ipv6 ipv6 = ((Ipv6Case) afi).getIpv6();
            PCEPEndPointsIpv6ObjectParser.serializeObject(processing, ignore, ipv6, buffer);
        } else if (afi instanceof Ipv4Case) {
            final Ipv4 ipv4 = ((Ipv4Case) afi).getIpv4();
            PCEPEndPointsIpv4ObjectParser.serializeObject(processing, ignore, ipv4, buffer);
        } else if (afi instanceof P2mpIpv4Case) {
            final P2mpIpv4 ipv4 = ((P2mpIpv4Case) afi).getP2mpIpv4();
            PCEPP2MPEndPointsIpv4ObjectParser.serializeObject(processing, ignore, ipv4, buffer);
        } else if (afi instanceof P2mpIpv6Case) {
            final P2mpIpv6 ipv6 = ((P2mpIpv6Case) afi).getP2mpIpv6();
            PCEPP2MPEndPointsIpv6ObjectParser.serializeObject(processing, ignore, ipv6, buffer);
        }
    }
}

