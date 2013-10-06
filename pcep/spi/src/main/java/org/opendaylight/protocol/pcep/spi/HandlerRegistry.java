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
	public AutoCloseable registerMessageHandler(Class<? extends Message> msgClass, int messageType, MessageHandler handler);
	public MessageHandler getMessageHandler(int messageType);
	public MessageHandler getMessageHandler(Message message);

	public AutoCloseable registerObjectHandler(Class<? extends Object> objClass, int objectClass, int objectType, ObjectHandler handler);
	public ObjectHandler getObjectHandler(int objectClass, int objectType);
	public ObjectHandler getObjectHandler(Object object);

	public AutoCloseable registerTlvHandler(Class<? extends Tlv> tlvClass, int tlvType, TlvHandler handler);
	public TlvHandler getTlvHandler(int tlvType);
	public TlvHandler getTlvHandler(Tlv tlv);
}
