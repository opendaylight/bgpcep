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
        for (PCEPExtensionProviderActivator e : currentExtensionDependency) {
            e.stop();
        }
    }

    public void start(final List<PCEPExtensionProviderActivator> extensionDependency) {
        for (PCEPExtensionProviderActivator e : extensionDependency) {
            e.start(delegate);
        }

        currentExtensionDependency = extensionDependency;
    }

    public void reconfigure(final List<PCEPExtensionProviderActivator> extensionDependency) {
        // Shutdown old ones first
        for (PCEPExtensionProviderActivator e : currentExtensionDependency) {
            if (!extensionDependency.contains(e)) {
                e.stop();
            }
        }

        // Start new ones
        for (PCEPExtensionProviderActivator e : extensionDependency) {
            if (!currentExtensionDependency.contains(e)) {
                e.start(delegate);
            }
        }

        currentExtensionDependency = extensionDependency;
    }

    @Override
    public LabelRegistry getLabelHandlerRegistry() {
        return delegate.getLabelHandlerRegistry();
    }

    @Override
    public MessageRegistry getMessageHandlerRegistry() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ObjectRegistry getObjectHandlerRegistry() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EROSubobjectRegistry getEROSubobjectHandlerRegistry() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RROSubobjectRegistry getRROSubobjectHandlerRegistry() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XROSubobjectRegistry getXROSubobjectHandlerRegistry() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TlvRegistry getTlvHandlerRegistry() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VendorInformationTlvRegistry getVendorInformationTlvRegistry() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VendorInformationObjectRegistry getVendorInformationObjectRegistry() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AutoCloseable registerLabelSerializer(final Class<? extends LabelType> labelClass, final LabelSerializer serializer) {
        return delegate.registerLabelSerializer(labelClass, serializer);
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
    public AutoCloseable registerEROSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass,
            final EROSubobjectSerializer serializer) {
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
    public AutoCloseable registerRROSubobjectSerializer(
            final Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.SubobjectType> subobjectClass,
            final RROSubobjectSerializer serializer) {
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
    public AutoCloseable registerVendorInformationTlvSerializer(
            final Class<? extends EnterpriseSpecificInformation> esInformationClass, final TlvSerializer serializer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AutoCloseable registerVendorInformationTlvParser(final EnterpriseNumber enterpriseNumber, final TlvParser parser) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AutoCloseable registerXROSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass,
            final XROSubobjectSerializer serializer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AutoCloseable registerXROSubobjectParser(final int subobjectType, final XROSubobjectParser parser) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AutoCloseable registerVendorInformationObjectSerializer(
            final Class<? extends EnterpriseSpecificInformation> esInformationClass, final ObjectSerializer serializer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AutoCloseable registerVendorInformationObjectParser(final EnterpriseNumber enterpriseNumber, final ObjectParser parser) {
        // TODO Auto-generated method stub
        return null;
    }

}
