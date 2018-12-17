/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

public final class VendorInformationUtil {

    public static final int VENDOR_INFORMATION_TLV_TYPE = 7;

    public static final int VENDOR_INFORMATION_OBJECT_CLASS = 34;

    public static final int VENDOR_INFORMATION_OBJECT_TYPE = 1;

    private VendorInformationUtil() {
    }

    public static boolean isVendorInformationTlv(final int type) {
        return type == VENDOR_INFORMATION_TLV_TYPE;
    }

    public static boolean isVendorInformationObject(final int objectClass, final int objectType) {
        return objectType == VENDOR_INFORMATION_OBJECT_TYPE && objectClass == VENDOR_INFORMATION_OBJECT_CLASS;
    }
}
