/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.EnterpriseSpecificInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;

public interface PCEPExtensionProviderContext extends PCEPExtensionConsumerContext {
    AutoCloseable registerLabelSerializer(Class<? extends LabelType> labelClass, LabelSerializer serializer);

    AutoCloseable registerLabelParser(int ctype, LabelParser parser);

    AutoCloseable registerEROSubobjectParser(int subobjectType, EROSubobjectParser parser);

    AutoCloseable registerEROSubobjectSerializer(Class<? extends SubobjectType> subobjectClass,
            EROSubobjectSerializer serializer);

    AutoCloseable registerMessageParser(int messageType, MessageParser parser);

    AutoCloseable registerMessageSerializer(Class<? extends Message> msgClass, MessageSerializer serializer);

    AutoCloseable registerObjectParser(ObjectParser parser);

    @Deprecated
    AutoCloseable registerObjectParser(int objectClass, int objectType, ObjectParser parser);

    AutoCloseable registerObjectSerializer(Class<? extends Object> objClass, ObjectSerializer serializer);

    AutoCloseable registerRROSubobjectParser(int subobjectType, RROSubobjectParser parser);

    AutoCloseable registerRROSubobjectSerializer(Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params
            .xml.ns.yang.rsvp.rev150820.record.route.subobjects.SubobjectType> subobjectClass,
            RROSubobjectSerializer serializer);

    AutoCloseable registerTlvSerializer(Class<? extends Tlv> tlvClass, TlvSerializer serializer);

    AutoCloseable registerTlvParser(int tlvType, TlvParser parser);

    AutoCloseable registerVendorInformationTlvSerializer(
            Class<? extends EnterpriseSpecificInformation> esInformationClass, TlvSerializer serializer);

    AutoCloseable registerVendorInformationTlvParser(EnterpriseNumber enterpriseNumber, TlvParser parser);

    AutoCloseable registerXROSubobjectSerializer(Class<? extends SubobjectType> subobjectClass,
            XROSubobjectSerializer serializer);

    AutoCloseable registerXROSubobjectParser(int subobjectType, XROSubobjectParser parser);

    AutoCloseable registerVendorInformationObjectSerializer(
            Class<? extends EnterpriseSpecificInformation> esInformationClass, ObjectSerializer serializer);

    AutoCloseable registerVendorInformationObjectParser(EnterpriseNumber enterpriseNumber, ObjectParser parser);
}
