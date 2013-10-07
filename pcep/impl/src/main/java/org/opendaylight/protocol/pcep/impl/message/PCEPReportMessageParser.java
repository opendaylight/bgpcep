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
import org.opendaylight.protocol.pcep.message.PCEPReportMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.message.PCEPReportMessage PCEPReportMessage}
 */
public class PCEPReportMessageParser implements PCEPMessageParser {

	@Override
	public byte[] put(final Message msg) {
		if (!(msg instanceof PCEPReportMessage))
			throw new IllegalArgumentException("Wrong instance of PCEPMessage. Passed instance of " + msg.getClass()
					+ ". Nedded PCEPReportMessage.");

		return PCEPObjectFactory.put(((PCEPReportMessage) msg).getAllObjects());
	}

}
