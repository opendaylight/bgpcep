/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import org.opendaylight.protocol.pcep.impl.PCEPMessageParser;
import org.opendaylight.protocol.pcep.impl.PCEPObjectFactory;
import org.opendaylight.protocol.pcep.message.PCCreateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

/**
 * Parser for {@link PCCreateMessage}
 */
public class PCCreateMessageParser implements PCEPMessageParser {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.opendaylight.protocol.pcep.impl.PCEPMessageParser#put(org.opendaylight.protocol.pcep.PCEPMessage
	 * )
	 */
	@Override
	public byte[] put(final Message msg) {
		if (!(msg instanceof PCCreateMessage))
			throw new IllegalArgumentException("Wrong instance of PCEPMessage. Passed instance of " + msg.getClass()
					+ ". Needed PCCreateMessage.");

		return PCEPObjectFactory.put(((PCCreateMessage) msg).getAllObjects());
	}

}
