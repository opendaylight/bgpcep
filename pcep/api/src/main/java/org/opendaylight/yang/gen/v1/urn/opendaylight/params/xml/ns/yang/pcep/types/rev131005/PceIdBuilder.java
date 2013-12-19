package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005;

import com.google.common.net.InetAddresses;


/**
 **/
public final class PceIdBuilder {
	private PceIdBuilder() {

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
