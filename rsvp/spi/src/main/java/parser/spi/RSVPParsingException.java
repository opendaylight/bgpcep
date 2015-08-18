package parser.spi;

/**
 *
 * Used when something occurs during the parsing to get Update Message.
 *
 */
public class RSVPParsingException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates new BGPParsingException with specific error message.
     *
     * @param err error message string.
     */
    public RSVPParsingException(final String err) {
        super(err);
    }

    /**
     * Creates new BGPParsingException with specific message and cause.
     *
     * @param message exception message
     * @param cause primary exception
     */
    public RSVPParsingException(final String message, final Exception cause) {
        super(message, cause);
    }
}
