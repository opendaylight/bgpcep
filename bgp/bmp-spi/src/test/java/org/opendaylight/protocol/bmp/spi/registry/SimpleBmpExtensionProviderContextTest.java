package org.opendaylight.protocol.bmp.spi.registry;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.description.tlv.DescriptionTlv;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class SimpleBmpExtensionProviderContextTest {

    private static final SimpleBmpMessageRegistry MESSAGE_REGISTRY = new SimpleBmpMessageRegistry();
    private static final SimpleBmpExtensionProviderContext CONTEXT = new SimpleBmpExtensionProviderContext();

    @Test
    public void testRegisterBmpMessageParser() {
        assertNotNull(CONTEXT.registerBmpMessageParser(1, MESSAGE_REGISTRY));
    }

    @Test
    public void testRegisterBmpMessageSerializer() {
        assertNotNull(CONTEXT.registerBmpMessageSerializer(SimpleBmpMessageRegistryTest.BmpTestMessage.class, MESSAGE_REGISTRY));
    }

    @Test
    public void testGetBmpMessageRegistry() {
        assertNotNull(CONTEXT.getBmpMessageRegistry());
    }

    @Test
    public void testRegisterBmpStatisticsTlvParser() {
        assertNotNull(CONTEXT.registerBmpStatisticsTlvParser(1, SimpleBmpTlvRegistryTest.bmpTlvParser));
    }

    @Test
    public void testRegisterBmpStatisticsTlvSerializer() {
        assertNotNull(CONTEXT.registerBmpStatisticsTlvSerializer(SimpleDescriptionTlv.class, SimpleBmpTlvRegistryTest.bmpTlvSerializer));
    }

    @Test
    public void testRegisterBmpInitiationTlvParser() {
        assertNotNull(CONTEXT.registerBmpInitiationTlvParser(1, SimpleBmpTlvRegistryTest.bmpTlvParser));
    }

    @Test
    public void testRegisterBmpInitiationTlvSerializer() {
        assertNotNull(CONTEXT.registerBmpInitiationTlvSerializer(SimpleDescriptionTlv.class, SimpleBmpTlvRegistryTest.bmpTlvSerializer));
    }

    @Test
    public void testRegisterBmpTerminationTlvParser() {
        assertNotNull(CONTEXT.registerBmpTerminationTlvParser(1, SimpleBmpTlvRegistryTest.bmpTlvParser));
    }

    @Test
    public void testRegisterBmpTerminationTlvSerializer() {
        assertNotNull(CONTEXT.registerBmpTerminationTlvSerializer(SimpleDescriptionTlv.class, SimpleBmpTlvRegistryTest.bmpTlvSerializer));
    }

    @Test
    public void tetsGetBmpStatisticsTlvRegistry() {
        assertNotNull(CONTEXT.getBmpStatisticsTlvRegistry());
    }

    @Test
    public void testGetBmpInitiationTlvRegistry() {
        assertNotNull(CONTEXT.getBmpInitiationTlvRegistry());
    }

    @Test
    public void testGetBmpTerminationTlvRegistry() {
        assertNotNull(CONTEXT.getBmpTerminationTlvRegistry());
    }

    private static final class SimpleDescriptionTlv implements DescriptionTlv {

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return null;
        }

        @Override
        public <E extends Augmentation<DescriptionTlv>> E getAugmentation(
                Class<E> augmentationType) {
            return null;
        }

        @Override
        public String getDescription() {
            return "test";
        }

    }
}
