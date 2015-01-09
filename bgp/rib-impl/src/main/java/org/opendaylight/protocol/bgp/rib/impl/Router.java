package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.primitives.UnsignedInteger;

abstract class Router {
    abstract UnsignedInteger getRouterId();
}
