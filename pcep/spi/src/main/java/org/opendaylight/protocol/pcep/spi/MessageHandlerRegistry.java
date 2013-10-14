/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

public interface MessageHandlerRegistry {
	public AutoCloseable registerMessageParser(int messageType, MessageParser parser);
	public AutoCloseable registerMessageSerializer(Class<? extends Message> msgClass, MessageSerializer serializer);

	public MessageParser getMessageParser(int messageType);
	public MessageSerializer getMessageSerializer(Message message);
}
