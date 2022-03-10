/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object.unreach;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.unreach.destination.object.UnreachDestinationObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.unreach.destination.object.unreach.destination.obj.Destination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.unreach.destination.object.unreach.destination.obj.destination.Ipv4DestinationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.unreach.destination.object.unreach.destination.obj.destination.Ipv6DestinationCase;

public final class PCEPUnreachDestinationSerializer implements ObjectSerializer {
    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof UnreachDestinationObj,
            "Wrong instance of PCEPObject. Passed %s. Needed UnreachDestinationObj.", object.getClass());
        final UnreachDestinationObj uPObj = (UnreachDestinationObj) object;
        final Destination destination = uPObj.getDestination();
        final Boolean processing = object.getProcessingRule();
        final Boolean ignore = object.getIgnore();
        if (destination instanceof Ipv6DestinationCase) {
            final Ipv6DestinationCase ipv6 = (Ipv6DestinationCase) destination;
            PCEPIpv6UnreachDestinationParser.serializeObject(processing, ignore, ipv6, buffer);
        } else if (destination instanceof Ipv4DestinationCase) {
            final Ipv4DestinationCase ipv4 = (Ipv4DestinationCase) destination;
            PCEPIpv4UnreachDestinationParser.serializeObject(processing, ignore, ipv4, buffer);
        }
    }
}