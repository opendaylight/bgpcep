/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.impl.PCEPTlvParser;
import org.opendaylight.protocol.pcep.tlv.LSPUpdateErrorTlv;

public class LSPUpdateErrorTlvParser implements PCEPTlvParser {
	
	public static final int TYPE = 20;
	
	private static final int UPDATE_ERR_CODE_LENGTH = 4;

	@Override
	public PCEPTlv parse(byte[] valueBytes) throws PCEPDeserializerException {
		return new LSPUpdateErrorTlv(valueBytes);
	}

	@Override
	public byte[] put(PCEPTlv obj) {
		byte[] valueBytes = ((LSPUpdateErrorTlv) obj).getErrorCode();
		
	    assert valueBytes.length == UPDATE_ERR_CODE_LENGTH : "Update error code si too large.";
	    
	    return valueBytes;
	}

}
