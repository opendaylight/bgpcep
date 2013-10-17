/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.osgi;

import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.protocol.pcep.spi.MessageParser;
import org.opendaylight.protocol.pcep.spi.MessageSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.osgi.framework.BundleContext;

class OSGiPCEPExtensionProviderContext extends OSGiPCEPExtensionConsumerContext implements PCEPExtensionProviderContext {
	OSGiPCEPExtensionProviderContext(final BundleContext context) {
		super(context);
	}

	@Override
	public AutoCloseable registerLabelSerializer(final Class<? extends CLabel> labelClass, final LabelSerializer serializer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerLabelParser(final int cType, final LabelParser parser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerEROSubobjectParser(final int subobjectType, final EROSubobjectParser parser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerEROSubobjectSerializer(final Class<? extends CSubobject> subobjectClass, final EROSubobjectSerializer serializer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerMessageParser(final int messageType, final MessageParser parser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerMessageSerializer(final Class<? extends Message> msgClass, final MessageSerializer serializer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerObjectParser(final int objectClass, final int objectType, final ObjectParser parser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerObjectSerializer(final Class<? extends Object> objClass, final ObjectSerializer serializer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerRROSubobjectParser(final int subobjectType, final RROSubobjectParser parser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerRROSubobjectSerializer(final Class<? extends CSubobject> subobjectClass, final RROSubobjectSerializer serializer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerTlvSerializer(final Class<? extends Tlv> tlvClass, final TlvSerializer serializer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerTlvParser(final int tlvType, final TlvParser parser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerXROSubobjectSerializer(final Class<? extends CSubobject> subobjectClass, final XROSubobjectSerializer serializer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerXROSubobjectParser(final int subobjectType, final XROSubobjectParser parser) {
		// TODO Auto-generated method stub
		return null;
	}
}
