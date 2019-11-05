/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelRegistry;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.protocol.pcep.spi.MessageParser;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.EnterpriseSpecificInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;
import org.opendaylight.yangtools.concepts.Registration;

// This class is thread-safe
public class SimplePCEPExtensionProviderContext implements PCEPExtensionProviderContext {
    private final SimpleLabelRegistry labelReg = new SimpleLabelRegistry();
    private final SimpleMessageRegistry msgReg = new SimpleMessageRegistry();
    private final SimpleVendorInformationObjectRegistry viObjReg = new SimpleVendorInformationObjectRegistry();
    private final SimpleObjectRegistry objReg = new SimpleObjectRegistry(this.viObjReg);
    private final SimpleEROSubobjectRegistry eroSubReg = new SimpleEROSubobjectRegistry();
    private final SimpleRROSubobjectRegistry rroSubReg = new SimpleRROSubobjectRegistry();
    private final SimpleXROSubobjectRegistry xroSubReg = new SimpleXROSubobjectRegistry();
    private final SimpleTlvRegistry tlvReg = new SimpleTlvRegistry();
    private final SimpleVendorInformationTlvRegistry viTlvReg = new SimpleVendorInformationTlvRegistry();

    @Override
    public final LabelRegistry getLabelHandlerRegistry() {
        return this.labelReg;
    }

    @Override
    public final MessageRegistry getMessageHandlerRegistry() {
        return this.msgReg;
    }

    @Override
    public final ObjectRegistry getObjectHandlerRegistry() {
        return this.objReg;
    }

    @Override
    public final EROSubobjectRegistry getEROSubobjectHandlerRegistry() {
        return this.eroSubReg;
    }

    @Override
    public final RROSubobjectRegistry getRROSubobjectHandlerRegistry() {
        return this.rroSubReg;
    }

    @Override
    public final XROSubobjectRegistry getXROSubobjectHandlerRegistry() {
        return this.xroSubReg;
    }

    @Override
    public final TlvRegistry getTlvHandlerRegistry() {
        return this.tlvReg;
    }

    @Override
    public VendorInformationTlvRegistry getVendorInformationTlvRegistry() {
        return this.viTlvReg;
    }

    @Override
    public final Registration registerLabelSerializer(final Class<? extends LabelType> labelClass,
            final LabelSerializer serializer) {
        return this.labelReg.registerLabelSerializer(labelClass, serializer);
    }

    @Override
    public final Registration registerLabelParser(final int ctype, final LabelParser parser) {
        return this.labelReg.registerLabelParser(ctype, parser);
    }

    @Override
    public final Registration registerEROSubobjectParser(final int subobjectType, final EROSubobjectParser parser) {
        return this.eroSubReg.registerSubobjectParser(subobjectType, parser);
    }

    @Override
    public final Registration registerEROSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass,
            final EROSubobjectSerializer serializer) {
        return this.eroSubReg.registerSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public final Registration registerMessageParser(final int messageType, final MessageParser parser) {
        return this.msgReg.registerMessageParser(messageType, parser);
    }

    @Override
    public final Registration registerMessageSerializer(final Class<? extends Message> msgClass,
            final MessageSerializer serializer) {
        return this.msgReg.registerMessageSerializer(msgClass, serializer);
    }

    @Override
    public final Registration registerObjectParser(final ObjectParser parser) {
        return this.objReg.registerObjectParser(parser.getObjectClass(), parser.getObjectType(), parser);
    }

    @Override
    public final Registration registerObjectSerializer(final Class<? extends Object> objClass,
            final ObjectSerializer serializer) {
        return this.objReg.registerObjectSerializer(objClass, serializer);
    }

    @Override
    public final Registration registerRROSubobjectParser(final int subobjectType, final RROSubobjectParser parser) {
        return this.rroSubReg.registerSubobjectParser(subobjectType, parser);
    }

    @Override
    public final Registration registerRROSubobjectSerializer(final Class<? extends org.opendaylight.yang.gen.v1.urn
            .opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.SubobjectType> subobjectClass,
            final RROSubobjectSerializer serializer) {
        return this.rroSubReg.registerSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public Registration registerTlvParser(final int tlvType, final TlvParser parser) {
        return this.tlvReg.registerTlvParser(tlvType, parser);
    }

    @Override
    public final Registration registerTlvSerializer(final Class<? extends Tlv> tlvClass,
            final TlvSerializer serializer) {
        return this.tlvReg.registerTlvSerializer(tlvClass, serializer);
    }

    @Override
    public final Registration registerXROSubobjectParser(final int subobjectType, final XROSubobjectParser parser) {
        return this.xroSubReg.registerSubobjectParser(subobjectType, parser);
    }

    @Override
    public final Registration registerXROSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass,
            final XROSubobjectSerializer serializer) {
        return this.xroSubReg.registerSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public Registration registerVendorInformationTlvSerializer(final
            Class<? extends EnterpriseSpecificInformation> esInformationClass, final TlvSerializer serializer) {
        return this.viTlvReg.registerVendorInformationTlvSerializer(esInformationClass, serializer);
    }

    @Override
    public Registration registerVendorInformationTlvParser(final EnterpriseNumber enterpriseNumber,
            final TlvParser parser) {
        return this.viTlvReg.registerVendorInformationTlvParser(enterpriseNumber, parser);
    }

    @Override
    public Registration registerVendorInformationObjectSerializer(
            final Class<? extends EnterpriseSpecificInformation> esInformationClass,
            final ObjectSerializer serializer) {
        return this.viObjReg.registerVendorInformationObjectSerializer(esInformationClass, serializer);
    }

    @Override
    public Registration registerVendorInformationObjectParser(final EnterpriseNumber enterpriseNumber,
            final ObjectParser parser) {
        return this.viObjReg.registerVendorInformationObjectParser(enterpriseNumber, parser);
    }

    @Override
    public VendorInformationObjectRegistry getVendorInformationObjectRegistry() {
        return this.viObjReg;
    }
}
