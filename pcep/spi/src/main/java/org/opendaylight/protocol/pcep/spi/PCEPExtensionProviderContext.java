/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.LabelType;

public interface PCEPExtensionProviderContext extends PCEPExtensionConsumerContext {
	public AutoCloseable registerLabelSerializer(Class<? extends LabelType> labelClass, LabelSerializer serializer);

	public AutoCloseable registerLabelParser(int cType, LabelParser parser);

	public AutoCloseable registerEROSubobjectParser(int subobjectType, EROSubobjectParser parser);

	public AutoCloseable registerEROSubobjectSerializer(Class<? extends CSubobject> subobjectClass, EROSubobjectSerializer serializer);

	public AutoCloseable registerMessageParser(int messageType, MessageParser parser);

	public AutoCloseable registerMessageSerializer(Class<? extends Message> msgClass, MessageSerializer serializer);

	public AutoCloseable registerObjectParser(int objectClass, int objectType, ObjectParser parser);

	public AutoCloseable registerObjectSerializer(Class<? extends Object> objClass, ObjectSerializer serializer);

	public AutoCloseable registerRROSubobjectParser(int subobjectType, RROSubobjectParser parser);

	public AutoCloseable registerRROSubobjectSerializer(Class<? extends CSubobject> subobjectClass, RROSubobjectSerializer serializer);

	public AutoCloseable registerTlvSerializer(Class<? extends Tlv> tlvClass, TlvSerializer serializer);

	public AutoCloseable registerTlvParser(int tlvType, TlvParser parser);

	public AutoCloseable registerXROSubobjectSerializer(Class<? extends CSubobject> subobjectClass, XROSubobjectSerializer serializer);

	public AutoCloseable registerXROSubobjectParser(int subobjectType, XROSubobjectParser parser);
}
