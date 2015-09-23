/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.spi;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Holds current configuration state for a particular DataObject type.
 * Each configuration object is identified by a ModuleKey of mapped
 * configuration module and an Identifier of the DataObject.
 *
 * @param <V> A type of DataBobject to which the configuration is binded.
 */
public interface BGPConfigHolder<V extends DataObject> {

    /**
     * Remove configuration stored configuration state. The stored configuration
     * is removed when configuration with such ModuleKey is present.
     * @param moduleKey ModuleKey identifies configuration to be removed.
     * @return True if configuration was removed successfully, otherwise false.
     */
    boolean remove(@Nonnull ModuleKey moduleKey);

    /**
     * Update or add a new configuration object. If configuration object is present for
     * such ModuleKey, current state is compared (equality) with proposed newValue. If not
     * equals, current configuration state is updated (replaced) with newValue.
     * If configuration object is not present for such ModuleKey, new configuration state is added.
     * @param moduleKey ModuleKey identifies configuration to be add/updated.
     * @param key ModuleKey identifies configuration to be added/updated.
     * @param newValue Proposed configuration state.
     * @return True if configuration was added/updated successfully, otherwise false.
     */
    boolean addOrUpdate(@Nonnull ModuleKey moduleKey, @Nonnull Identifier key, @Nonnull V newValue);

    /**
     * Retrieves ModuleKey for an input configuration object Identifier.
     * @param key A Configuration data object Identifier.
     * @return ModuleKey for an input Identifier, or null if not present.
     */
    ModuleKey getModuleKey(@Nonnull Identifier key);

    /**
     * Retrieves a configuration object Identifier for an input ModuleKey.
     * @param moduleKey A ModuleKey of data object.
     * @return Identifier for an input ModuleKey, or null if not present.
     */
    Identifier getKey(@Nonnull ModuleKey moduleKey);
}
