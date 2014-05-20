/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class LocalPreferenceAttributeParser implements AttributeParser,AttributeSerializer {
	public static final int TYPE = 5;
    public static final int ATTR_FLAGS = 64;
    public static final int LOCAL_PREFS_LENGTH = 4;

	@Override
	public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) {
		builder.setLocalPref(new LocalPrefBuilder().setPref(buffer.readUnsignedInt()).build());
	}

    @Override
    public void serializeAttribute(DataObject attribute, ByteBuf byteAggregator) {
        LocalPref localPref = (LocalPref) attribute;

        byteAggregator.writeByte(UnsignedBytes.checkedCast(ATTR_FLAGS));
        byteAggregator.writeByte(UnsignedBytes.checkedCast(TYPE));
        byteAggregator.writeByte(UnsignedBytes.checkedCast(LOCAL_PREFS_LENGTH));

        byteAggregator.writeInt(localPref.getPref().shortValue());
    }

}