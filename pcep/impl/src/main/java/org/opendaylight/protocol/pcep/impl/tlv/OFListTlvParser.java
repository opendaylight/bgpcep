/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import io.netty.buffer.ByteBuf;

import java.util.List;

import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfListBuilder;

import com.google.common.collect.Lists;

/**
 * Parser for {@link OfList}
 */
public class OFListTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 4;

	private static final int OF_CODE_ELEMENT_LENGTH = 2;

	@Override
	public OfList parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
		if (buffer == null) {
			return null;
		}
		if (buffer.readableBytes() % OF_CODE_ELEMENT_LENGTH != 0) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ".");
		}
		final List<OfId> ofCodes = Lists.newArrayList();
		while (buffer.isReadable()) {
			ofCodes.add(new OfId(buffer.readUnsignedShort()));
		}
		return new OfListBuilder().setCodes(ofCodes).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null) {
			throw new IllegalArgumentException("OFListTlv is mandatory.");
		}
		final OfList oft = (OfList) tlv;

		final List<OfId> ofCodes = oft.getCodes();
		final byte[] retBytes = new byte[ofCodes.size() * OF_CODE_ELEMENT_LENGTH];

		final int size = ofCodes.size();
		for (int i = 0; i < size; i++) {
			ByteArray.copyWhole(ByteArray.shortToBytes(ofCodes.get(i).getValue().shortValue()), retBytes, i * OF_CODE_ELEMENT_LENGTH);
		}
		return TlvUtil.formatTlv(TYPE, retBytes);
	}
}
