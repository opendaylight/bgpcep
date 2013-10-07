/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspErrorCodeTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.tlvs.LspErrorCodeBuilder;

public class LspUpdateErrorTlvParser implements TlvParser {

	@Override
	public LspErrorCodeTlv parseTlv(byte[] buffer) throws PCEPDeserializerException {
		return new LspErrorCodeBuilder().setErrorCode(ByteArray.bytesToLong(buffer)).build();
	}
}
