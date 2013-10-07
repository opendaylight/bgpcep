/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPMessageValidator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.CloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.CloseMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.message.CCloseMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.message.c.close.message.CClose;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.message.c.close.message.CCloseBuilder;

/**
 * PCEPCloseMessage validator. Validates message integrity.
 */
public class PCEPCloseMessageValidator extends PCEPMessageValidator {

	@Override
	public List<Message> validate(final List<PCEPObject> objects) throws PCEPDeserializerException {
		if (objects == null)
			throw new IllegalArgumentException("Passed list can't be null.");

		if (objects.isEmpty() || !(objects.get(0) instanceof CClose))
			throw new PCEPDeserializerException("Close message doesn't contain CLOSE object.");

		final Object o = objects.get(0);
		final CloseMessage msg = new CloseBuilder().setCCloseMessage(
				new CCloseMessageBuilder().setCClose(new CCloseBuilder().setReason(((CClose) o).getReason()).build()).build()).build();
		objects.remove(0);

		if (!objects.isEmpty())
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);

		return new ArrayList<Message>() {
			private static final long serialVersionUID = 1L;
			{
				this.add(msg);
			}
		};
	}
}
