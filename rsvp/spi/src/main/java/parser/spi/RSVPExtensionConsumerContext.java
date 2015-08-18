package parser.spi;

public interface RSVPExtensionConsumerContext {
    RSVPTeObjectRegistry getRsvpRegistry();

    XROSubobjectRegistry getXROSubobjectHandlerRegistry();

    EROSubobjectRegistry getEROSubobjectHandlerRegistry();

    RROSubobjectRegistry getRROSubobjectHandlerRegistry();

    LabelRegistry getLabelHandlerRegistry();
}
