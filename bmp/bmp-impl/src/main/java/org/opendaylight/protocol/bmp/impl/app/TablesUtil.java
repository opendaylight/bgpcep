/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.app;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev200120.BmpMonitor;
import org.opendaylight.yangtools.yang.common.QName;

public final class TablesUtil {
    public static final QName BMP_TABLES_QNAME = QName.create(BmpMonitor.QNAME, "tables").intern();
    public static final QName BMP_ATTRIBUTES_QNAME = QName.create(BmpMonitor.QNAME, "attributes").intern();
    public static final QName BMP_ROUTES_QNAME = QName.create(BmpMonitor.QNAME, "routes").intern();
    public static final QName BMP_AFI_QNAME = QName.create(BMP_TABLES_QNAME, "afi").intern();
    public static final QName BMP_SAFI_QNAME = QName.create(BMP_TABLES_QNAME, "safi").intern();

    private TablesUtil() {
        // Hidden on purpose
    }

}
