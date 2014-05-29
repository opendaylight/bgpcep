/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class PathAttributeSerializer implements AttributeSerializer {

    private List<AttributeSerializer> pathAttributesSerializers = new ArrayList<AttributeSerializer>();

    public void registerSerializer(AttributeSerializer attributeSerializer){
        this.pathAttributesSerializers.add(attributeSerializer);
    }
    @Override
    public void serializeAttribute(DataObject attribute, ByteBuf byteAggregator) {
        PathAttributes pathAttributes = (PathAttributes) attribute;
        for (AttributeSerializer attributeSerializer:this.pathAttributesSerializers){
            attributeSerializer.serializeAttribute(pathAttributes,byteAggregator);
        }
    }
}
