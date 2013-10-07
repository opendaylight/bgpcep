/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKeyObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.path.key.expansion.PathKeyBuilder;

/**
 * Parser for {@link PathKeyObject}
 */
public class PCEPPathKeyObjectParser extends AbstractObjectParser<PathKeyBuilder> {

	public static final int CLASS = 16;

	public static final int TYPE = 1;

	public PCEPPathKeyObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public PathKeyObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
			PCEPDocumentedException {
		// FIXME : finish

		final PathKeyBuilder builder = new PathKeyBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		return builder.build();
	}

	@Override
	public void addTlv(final PathKeyBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof PathKeyObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed PathKeyObject.");

		final PathKeyObject pkey = (PathKeyObject) object;

		// FIXME, but no Tlvs defined
		// final byte[] tlvs = PCEPTlvParser.put(lspaObj.getTlvs());
		// final byte[] retBytes = new byte[TLVS_F_OFFSET + tlvs.length];
		// ByteArray.copyWhole(tlvs, retBytes, TLVS_F_OFFSET);
		return new byte[0];
	}

	@Override
	public int getObjectType() {
		return TYPE;
	}

	@Override
	public int getObjectClass() {
		return CLASS;
	}
}
