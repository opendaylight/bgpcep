/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;

public interface BGPConfigHolder<K, V> {

    boolean remove(final ModuleKey moduleKey);

    boolean addOrUpdate(final ModuleKey moduleKey, final K key, final V newValue);

    ModuleKey getModuleKey(final K key);
}
