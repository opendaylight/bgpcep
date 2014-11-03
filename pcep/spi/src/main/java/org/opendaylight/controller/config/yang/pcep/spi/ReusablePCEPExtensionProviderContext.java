package org.opendaylight.controller.config.yang.pcep.spi;

import java.util.List;
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
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
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
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.EnterpriseSpecificInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.LabelType;

public class ReusablePCEPExtensionProviderContext implements AutoCloseable, PCEPExtensionProviderContext {
    private final PCEPExtensionProviderContext delegate = new SimplePCEPExtensionProviderContext();
    private List<PCEPExtensionProviderActivator> currentExtensionDependency;

    @Override
    public void close() {
        for (final PCEPExtensionProviderActivator e : this.currentExtensionDependency) {
            e.stop();
        }
    }

    public void start(final List<PCEPExtensionProviderActivator> extensionDependency) {
        for (final PCEPExtensionProviderActivator e : extensionDependency) {
            e.start(this.delegate);
        }
        this.currentExtensionDependency = extensionDependency;
    }

    public void reconfigure(final List<PCEPExtensionProviderActivator> extensionDependency) {
        // Shutdown old ones first
        for (final PCEPExtensionProviderActivator e : this.currentExtensionDependency) {
            if (!extensionDependency.contains(e)) {
                e.stop();
            }
        }
        // Start new ones
        for (final PCEPExtensionProviderActivator e : extensionDependency) {
            if (!this.currentExtensionDependency.contains(e)) {
                e.start(this.delegate);
            }
        }
        this.currentExtensionDependency = extensionDependency;
    }

    @Override
    public LabelRegistry getLabelHandlerRegistry() {
        return this.delegate.getLabelHandlerRegistry();
    }

    @Override
    public MessageRegistry getMessageHandlerRegistry() {
        return this.delegate.getMessageHandlerRegistry();
    }

    @Override
    public ObjectRegistry getObjectHandlerRegistry() {
        return this.delegate.getObjectHandlerRegistry();
    }

    @Override
    public EROSubobjectRegistry getEROSubobjectHandlerRegistry() {
        return this.delegate.getEROSubobjectHandlerRegistry();
    }

    @Override
    public RROSubobjectRegistry getRROSubobjectHandlerRegistry() {
        return this.delegate.getRROSubobjectHandlerRegistry();
    }

    @Override
    public XROSubobjectRegistry getXROSubobjectHandlerRegistry() {
        return this.delegate.getXROSubobjectHandlerRegistry();
    }

    @Override
    public TlvRegistry getTlvHandlerRegistry() {
        return this.delegate.getTlvHandlerRegistry();
    }

    @Override
    public VendorInformationTlvRegistry getVendorInformationTlvRegistry() {
        return this.delegate.getVendorInformationTlvRegistry();
    }

    @Override
    public VendorInformationObjectRegistry getVendorInformationObjectRegistry() {
        return this.delegate.getVendorInformationObjectRegistry();
    }

    @Override
    public AutoCloseable registerLabelSerializer(final Class<? extends LabelType> labelClass, final LabelSerializer serializer) {
        return this.delegate.registerLabelSerializer(labelClass, serializer);
    }

    @Override
    public AutoCloseable registerLabelParser(final int cType, final LabelParser parser) {
        return this.delegate.registerLabelParser(cType, parser);
    }

    @Override
    public AutoCloseable registerEROSubobjectParser(final int subobjectType, final EROSubobjectParser parser) {
        return this.delegate.registerEROSubobjectParser(subobjectType, parser);
    }

    @Override
    public AutoCloseable registerEROSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass,
            final EROSubobjectSerializer serializer) {
        return this.delegate.registerEROSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public AutoCloseable registerMessageParser(final int messageType, final MessageParser parser) {
        return this.delegate.registerMessageParser(messageType, parser);
    }

    @Override
    public AutoCloseable registerMessageSerializer(final Class<? extends Message> msgClass, final MessageSerializer serializer) {
        return this.delegate.registerMessageSerializer(msgClass, serializer);
    }

    @Override
    public AutoCloseable registerObjectParser(final int objectClass, final int objectType, final ObjectParser parser) {
        return this.delegate.registerObjectParser(objectClass, objectType, parser);
    }

    @Override
    public AutoCloseable registerObjectSerializer(final Class<? extends Object> objClass, final ObjectSerializer serializer) {
        return this.delegate.registerObjectSerializer(objClass, serializer);
    }

    @Override
    public AutoCloseable registerRROSubobjectParser(final int subobjectType, final RROSubobjectParser parser) {
        return this.delegate.registerRROSubobjectParser(subobjectType, parser);
    }

    @Override
    public AutoCloseable registerRROSubobjectSerializer(
            final Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.SubobjectType> subobjectClass,
            final RROSubobjectSerializer serializer) {
        return this.delegate.registerRROSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public AutoCloseable registerTlvSerializer(final Class<? extends Tlv> tlvClass, final TlvSerializer serializer) {
        return this.delegate.registerTlvSerializer(tlvClass, serializer);
    }

    @Override
    public AutoCloseable registerTlvParser(final int tlvType, final TlvParser parser) {
        return this.delegate.registerTlvParser(tlvType, parser);
    }

    @Override
    public AutoCloseable registerVendorInformationTlvSerializer(
            final Class<? extends EnterpriseSpecificInformation> esInformationClass, final TlvSerializer serializer) {
        return this.delegate.registerVendorInformationTlvSerializer(esInformationClass, serializer);
    }

    @Override
    public AutoCloseable registerVendorInformationTlvParser(final EnterpriseNumber enterpriseNumber, final TlvParser parser) {
        return this.delegate.registerVendorInformationTlvParser(enterpriseNumber, parser);
    }

    @Override
    public AutoCloseable registerXROSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass,
            final XROSubobjectSerializer serializer) {
        return this.delegate.registerXROSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public AutoCloseable registerXROSubobjectParser(final int subobjectType, final XROSubobjectParser parser) {
        return this.delegate.registerXROSubobjectParser(subobjectType, parser);
    }

    @Override
    public AutoCloseable registerVendorInformationObjectSerializer(
            final Class<? extends EnterpriseSpecificInformation> esInformationClass, final ObjectSerializer serializer) {
        return this.delegate.registerVendorInformationObjectSerializer(esInformationClass, serializer);
    }

    @Override
    public AutoCloseable registerVendorInformationObjectParser(final EnterpriseNumber enterpriseNumber, final ObjectParser parser) {
        return this.delegate.registerVendorInformationObjectParser(enterpriseNumber, parser);
    }
}
