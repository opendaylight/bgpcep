package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820;


import com.google.common.net.InetAddresses;

/**
 * The purpose of generated class in src/main/java for Union types is to create new instances of unions from a string representation.
 * In some cases it is very difficult to automate it since there can be unions such as (uint32 - uint16), or (string - uint32).
 *
 * The reason behind putting it under src/main/java is:
 * This class is generated in form of a stub and needs to be finished by the user. This class is generated only once to prevent
 * loss of user code.
 *
 */
public class PceIdBuilder {
    private PceIdBuilder() {
        throw new UnsupportedOperationException();
    }

    /**
     * Construct a new PCE ID, from either an IPv4 or IPv6 form.
     *
     * @param defaultValue Which is a PCE-ID in string form
     * @return A PCE ID.
     */
    public static PceId getDefaultInstance(final String defaultValue) {
        return new PceId(InetAddresses.forString(defaultValue).getAddress());
    }
}
