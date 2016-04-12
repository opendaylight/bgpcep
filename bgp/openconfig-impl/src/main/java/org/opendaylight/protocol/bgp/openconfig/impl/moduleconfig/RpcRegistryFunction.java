/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.impl.moduleconfig;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.ServiceRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.ServiceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.service.Instance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.binding.rev131028.BindingRpcRegistry;
import org.opendaylight.yangtools.yang.binding.ChildOf;

public final class RpcRegistryFunction {

    private RpcRegistryFunction() {
        throw new UnsupportedOperationException();
    }

    public static <T extends ServiceRef & ChildOf<Module>> T getRpcRegistryInstance(final ReadOnlyTransaction rTx,
        final BGPConfigModuleProvider configModuleOp, final Function<String, T> function) {

        Preconditions.checkNotNull(rTx);
        try {
            final Optional<Service> maybeService = configModuleOp.readConfigService(new ServiceKey(BindingRpcRegistry.class), rTx);
            if (maybeService.isPresent()) {
                final List<Instance> list = maybeService.get().getInstance();
                if (!list.isEmpty()) {
                    return function.apply(list.get(0).getName());
                }
            }
            return null;
        } catch (final ReadFailedException e) {
            throw new IllegalStateException("Failed to read service.", e);
        }
    }
}
