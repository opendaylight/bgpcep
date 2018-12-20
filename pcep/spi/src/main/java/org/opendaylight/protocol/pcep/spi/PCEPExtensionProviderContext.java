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
import org.opendaylight.yangtools.concepts.Registration;

public interface PCEPExtensionProviderContext extends PCEPExtensionConsumerContext {
    Registration registerLabelSerializer(Class<? extends LabelType> labelClass, LabelSerializer serializer);

    Registration registerLabelParser(int ctype, LabelParser parser);

    Registration registerEROSubobjectParser(int subobjectType, EROSubobjectParser parser);

    Registration registerEROSubobjectSerializer(Class<? extends SubobjectType> subobjectClass,
            EROSubobjectSerializer serializer);

    Registration registerMessageParser(int messageType, MessageParser parser);

    Registration registerMessageSerializer(Class<? extends Message> msgClass, MessageSerializer serializer);

    Registration registerObjectParser(ObjectParser parser);

    @Deprecated
    Registration registerObjectParser(int objectClass, int objectType, ObjectParser parser);

    Registration registerObjectSerializer(Class<? extends Object> objClass, ObjectSerializer serializer);

    Registration registerRROSubobjectParser(int subobjectType, RROSubobjectParser parser);

    Registration registerRROSubobjectSerializer(Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params
            .xml.ns.yang.rsvp.rev150820.record.route.subobjects.SubobjectType> subobjectClass,
            RROSubobjectSerializer serializer);

    Registration registerTlvSerializer(Class<? extends Tlv> tlvClass, TlvSerializer serializer);

    Registration registerTlvParser(int tlvType, TlvParser parser);

    Registration registerVendorInformationTlvSerializer(
            Class<? extends EnterpriseSpecificInformation> esInformationClass, TlvSerializer serializer);

    Registration registerVendorInformationTlvParser(EnterpriseNumber enterpriseNumber, TlvParser parser);

    Registration registerXROSubobjectSerializer(Class<? extends SubobjectType> subobjectClass,
            XROSubobjectSerializer serializer);

    Registration registerXROSubobjectParser(int subobjectType, XROSubobjectParser parser);

    Registration registerVendorInformationObjectSerializer(
            Class<? extends EnterpriseSpecificInformation> esInformationClass, ObjectSerializer serializer);

    Registration registerVendorInformationObjectParser(EnterpriseNumber enterpriseNumber, ObjectParser parser);
}
