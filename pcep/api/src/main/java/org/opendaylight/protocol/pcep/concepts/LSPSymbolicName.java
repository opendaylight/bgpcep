/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.concepts;

import org.opendaylight.protocol.concepts.AbstractIdentifier;
import org.opendaylight.protocol.util.ByteArray;
import com.google.common.base.Objects.ToStringHelper;

/**
 * @see <a
 *      href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-7.2.1">The
 *      LSP Symbolic Name TLV</a>
 */
public final class LSPSymbolicName extends AbstractIdentifier<LSPSymbolicName> {

	private static final long serialVersionUID = -5649378295100912021L;

	private final byte[] symbolicName;

	/**
	 * Creates LSPSymbolicName using byte array as value.
	 *
	 * @param symbolicName
	 *            value of the LSPSymbolicName TLV
	 */
	public LSPSymbolicName(final byte[] symbolicName) {
		this.symbolicName = symbolicName;
	}

	/**
	 * Gets Symbolic Name in raw byte array representation.
	 *
	 * @return byte array representation of Symbolic Name. May be null.
	 */
	public byte[] getSymbolicName() {
		return this.symbolicName;
	}

	@Override
	protected byte[] getBytes() {
		return this.symbolicName;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper.add("symbolicName", ByteArray.toHexString(symbolicName, "."));
	}
}
