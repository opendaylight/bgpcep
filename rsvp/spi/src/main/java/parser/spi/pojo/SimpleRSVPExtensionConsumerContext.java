package parser.spi.pojo;

import parser.spi.RSVPExtensionConsumerContext;

public class SimpleRSVPExtensionConsumerContext implements RSVPExtensionConsumerContext {
    private final SimpleRSVPObjectRegistry rsvpRegistry = new SimpleRSVPObjectRegistry();
    private final SimpleXROSubobjectRegistry xroSubReg = new SimpleXROSubobjectRegistry();
    private final SimpleRROSubobjectRegistry rroSubReg = new SimpleRROSubobjectRegistry();
    private final SimpleEROSubobjectRegistry eroSubReg = new SimpleEROSubobjectRegistry();
    private final SimpleLabelRegistry labelRegistry = new SimpleLabelRegistry();

    @Override
    public final SimpleRSVPObjectRegistry getRsvpRegistry() {
        return this.rsvpRegistry;
    }

    @Override
    public final SimpleXROSubobjectRegistry getXROSubobjectHandlerRegistry() {
        return this.xroSubReg;
    }

    @Override
    public SimpleEROSubobjectRegistry getEROSubobjectHandlerRegistry() {
        return this.eroSubReg;
    }

    @Override
    public final SimpleRROSubobjectRegistry getRROSubobjectHandlerRegistry() {
        return this.rroSubReg;
    }

    @Override
    public final SimpleLabelRegistry getLabelHandlerRegistry() {
        return this.labelRegistry;
    }
}
