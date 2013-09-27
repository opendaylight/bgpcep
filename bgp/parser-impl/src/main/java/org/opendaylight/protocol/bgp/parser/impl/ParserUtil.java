package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public final class ParserUtil {

	private ParserUtil() { }

	public static Class<? extends AddressFamily> afiForValue(final int value) {
		// FIXME: this needs to be extensible through an SPI interface
		switch (value) {
		case 1:
			return Ipv4AddressFamily.class;
		case 2:
			return Ipv6AddressFamily.class;
		case 16388:
			return LinkstateAddressFamily.class;
		}

		return null;
	}

	public static Class<? extends SubsequentAddressFamily> safiForValue(final int value) {
		// FIXME: this needs to be extensible through an SPI interface
		switch (value) {
		case 1:
			return UnicastSubsequentAddressFamily.class;
		case 128:
			return MplsLabeledVpnSubsequentAddressFamily.class;
		case 71:
			return LinkstateSubsequentAddressFamily.class;
		}

		return null;
	}

	public static int valueForSafi(final Class<? extends SubsequentAddressFamily> safi) {
		// FIXME: this needs to be extensible through an SPI interface
		if (safi == UnicastSubsequentAddressFamily.class) {
			return 1;
		} else if (safi == MplsLabeledVpnSubsequentAddressFamily.class) {
			return 128;
		} else if (safi == LinkstateSubsequentAddressFamily.class) {
			return 71;
		} else {
			throw new IllegalArgumentException("Unhandled SAFI class " + safi.getClass());
		}
	}

	public static int valueForAfi(final Class<? extends AddressFamily> afi) {
		// FIXME: this needs to be extensible through an SPI interface
		if (afi == Ipv4AddressFamily.class) {
			return 1;
		} else if (afi == Ipv6AddressFamily.class) {
			return 2;
		} else if (afi == LinkstateAddressFamily.class) {
			return 16388;
		} else {
			throw new IllegalArgumentException("Unhandled AFI class " + afi.getClass());
		}
	}
}
