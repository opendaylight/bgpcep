/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import org.opendaylight.protocol.pcep.MessageRegistry;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelRegistry;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.protocol.pcep.spi.MessageParser;
import org.opendaylight.protocol.pcep.spi.MessageSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.VendorInformationObjectRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.message.MessageType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.vendor.information.EnterpriseSpecificInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;
import org.opendaylight.yangtools.concepts.Registration;

// This class is thread-safe
public final class SimplePCEPExtensionProviderContext implements PCEPExtensionProviderContext {
    private final SimpleLabelRegistry labelReg = new SimpleLabelRegistry();
    private final SimpleMessageRegistry msgReg = new SimpleMessageRegistry();
    private final SimpleVendorInformationObjectRegistry viObjReg = new SimpleVendorInformationObjectRegistry();
    private final SimpleObjectRegistry objReg = new SimpleObjectRegistry(viObjReg);
    private final SimpleEROSubobjectRegistry eroSubReg = new SimpleEROSubobjectRegistry();
    private final SimpleRROSubobjectRegistry rroSubReg = new SimpleRROSubobjectRegistry();
    private final SimpleXROSubobjectRegistry xroSubReg = new SimpleXROSubobjectRegistry();
    private final SimpleTlvRegistry tlvReg = new SimpleTlvRegistry();
    private final SimpleVendorInformationTlvRegistry viTlvReg = new SimpleVendorInformationTlvRegistry();

    @Override
    public LabelRegistry getLabelHandlerRegistry() {
        return labelReg;
    }

    @Override
    public MessageRegistry getMessageHandlerRegistry() {
        return msgReg;
    }

    @Override
    public ObjectRegistry getObjectHandlerRegistry() {
        return objReg;
    }

    @Override
    public EROSubobjectRegistry getEROSubobjectHandlerRegistry() {
        return eroSubReg;
    }

    @Override
    public RROSubobjectRegistry getRROSubobjectHandlerRegistry() {
        return rroSubReg;
    }

    @Override
    public XROSubobjectRegistry getXROSubobjectHandlerRegistry() {
        return xroSubReg;
    }

    @Override
    public TlvRegistry getTlvHandlerRegistry() {
        return tlvReg;
    }

    @Override
    public VendorInformationTlvRegistry getVendorInformationTlvRegistry() {
        return viTlvReg;
    }

    @Override
    public Registration registerLabelSerializer(final Class<? extends LabelType> labelClass,
            final LabelSerializer serializer) {
        return labelReg.registerLabelSerializer(labelClass, serializer);
    }

    @Override
    public Registration registerLabelParser(final int ctype, final LabelParser parser) {
        return labelReg.registerLabelParser(ctype, parser);
    }

    @Override
    public Registration registerEROSubobjectParser(final int subobjectType, final EROSubobjectParser parser) {
        return eroSubReg.registerSubobjectParser(subobjectType, parser);
    }

    @Override
    public Registration registerEROSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass,
            final EROSubobjectSerializer serializer) {
        return eroSubReg.registerSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public Registration registerMessageParser(final int messageType, final MessageParser parser) {
        return msgReg.registerMessageParser(messageType, parser);
    }

    @Override
    public Registration registerMessageSerializer(final Class<? extends MessageType> msgClass,
            final MessageSerializer serializer) {
        return msgReg.registerMessageSerializer(msgClass, serializer);
    }

    @Override
    public Registration registerObjectParser(final ObjectParser parser) {
        return objReg.registerObjectParser(parser.getObjectClass(), parser.getObjectType(), parser);
    }

    @Override
    public Registration registerObjectSerializer(final Class<? extends Object> objClass,
            final ObjectSerializer serializer) {
        return objReg.registerObjectSerializer(objClass, serializer);
    }

    @Override
    public Registration registerRROSubobjectParser(final int subobjectType, final RROSubobjectParser parser) {
        return rroSubReg.registerSubobjectParser(subobjectType, parser);
    }

    @Override
    public Registration registerRROSubobjectSerializer(final Class<? extends org.opendaylight.yang.gen.v1.urn
            .opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.SubobjectType> subobjectClass,
            final RROSubobjectSerializer serializer) {
        return rroSubReg.registerSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public Registration registerTlvParser(final int tlvType, final TlvParser parser) {
        return tlvReg.registerTlvParser(tlvType, parser);
    }

    @Override
    public Registration registerTlvSerializer(final Class<? extends Tlv> tlvClass,
            final TlvSerializer serializer) {
        return tlvReg.registerTlvSerializer(tlvClass, serializer);
    }

    @Override
    public Registration registerXROSubobjectParser(final int subobjectType, final XROSubobjectParser parser) {
        return xroSubReg.registerSubobjectParser(subobjectType, parser);
    }

    @Override
    public Registration registerXROSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass,
            final XROSubobjectSerializer serializer) {
        return xroSubReg.registerSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public Registration registerVendorInformationTlvSerializer(final
            Class<? extends EnterpriseSpecificInformation> esInformationClass, final TlvSerializer serializer) {
        return viTlvReg.registerVendorInformationTlvSerializer(esInformationClass, serializer);
    }

    @Override
    public Registration registerVendorInformationTlvParser(final EnterpriseNumber enterpriseNumber,
            final TlvParser parser) {
        return viTlvReg.registerVendorInformationTlvParser(enterpriseNumber, parser);
    }

    @Override
    public Registration registerVendorInformationObjectSerializer(
            final Class<? extends EnterpriseSpecificInformation> esInformationClass,
            final ObjectSerializer serializer) {
        return viObjReg.registerVendorInformationObjectSerializer(esInformationClass, serializer);
    }

    @Override
    public Registration registerVendorInformationObjectParser(final EnterpriseNumber enterpriseNumber,
            final ObjectParser parser) {
        return viObjReg.registerVendorInformationObjectParser(enterpriseNumber, parser);
    }

    @Override
    public VendorInformationObjectRegistry getVendorInformationObjectRegistry() {
        return viObjReg;
    }
}
