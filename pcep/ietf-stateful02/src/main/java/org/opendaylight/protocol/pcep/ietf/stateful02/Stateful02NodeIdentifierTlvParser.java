/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful02;

import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.node.identifier.tlv.NodeIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

public final class Stateful02NodeIdentifierTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 24;

	@Override
	public int getType() {
		return TYPE;
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		return ((NodeIdentifier) tlv).getValue();
	}

	@Override
	public Tlv parseTlv(final byte[] buffer) throws PCEPDeserializerException {
		return new NodeIdentifierBuilder().setNodeId(new NodeIdentifier(buffer)).build();
	}
}