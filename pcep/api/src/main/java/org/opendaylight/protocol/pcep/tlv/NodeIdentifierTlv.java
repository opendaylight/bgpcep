/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.tlv;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

/**
 * Structure of Node Identifier TLV.
 *
 * @see draft-ietf-pce-stateful-pce-01 (sec. 7.1.3) - NODE_IDENTIFIER_TLV
 */
public class NodeIdentifierTlv implements Tlv {
	private final byte[] value;

	/**
	 * Constructs new Node Identifier TLV.
	 *
	 * @param value
	 *            byte[]
	 */
	public NodeIdentifierTlv(byte[] value) {
		if (value == null)
			throw new IllegalArgumentException("Value is mandatory.");
		if (value.length == 0)
			throw new IllegalArgumentException("Value has to be long at least 1 byte.");

		this.value = value;
	}

	/**
	 * Gets value of Node Identifier TLV as Bytes Array.
	 *
	 * @return byte[]
	 */
	public byte[] getValue() {
		return this.value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(this.value);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final NodeIdentifierTlv other = (NodeIdentifierTlv) obj;
		if (!Arrays.equals(this.value, other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("NodeIdentifierTlv [value=");
		try {
			builder.append(Charset.forName("UTF-8").newDecoder().decode(ByteBuffer.wrap(this.value)).toString());
		} catch (final CharacterCodingException e) {
			builder.append(Arrays.toString(this.value));
		}
		builder.append("]");
		return builder.toString();
	}
}
