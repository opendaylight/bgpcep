/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

public interface HandlerRegistry {
	public AutoCloseable registerMessageParser(int messageType, MessageParser parser);
	public MessageParser getMessageParser(int messageType);

	public AutoCloseable registerMessageSerializer(Class<? extends Message> msgClass, MessageSerializer serializer);
	public MessageSerializer getMessageSerializer(Message message);

	public AutoCloseable registerObjectParser(int objectClass, int objectType, ObjectParser parser);
	public ObjectParser getObjectParser(int objectClass, int objectType);

	public AutoCloseable registerObjectSerializer(Class<? extends Object> objClass, ObjectSerializer serializer);
	public ObjectSerializer getObjectSerializer(Object object);

	public AutoCloseable registerTlvParser(int tlvType, TlvParser parser);
	public TlvParser getTlvParser(int tlvType);

	public AutoCloseable registerTlvSerializer(Class<? extends Tlv> tlvClass, TlvSerializer serializer);
	public TlvSerializer getTlvSerializer(Tlv tlv);
}
