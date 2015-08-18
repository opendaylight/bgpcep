package parser.spi;

public interface RSVPExtensionProviderActivator {
    void start(RSVPExtensionProviderContext context);

    void stop();
}
