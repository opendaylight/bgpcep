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
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.ServiceRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.ServiceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.service.Instance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.dom.rev131028.DomAsyncDataBroker;
import org.opendaylight.yangtools.yang.binding.ChildOf;

@SuppressWarnings("ALL")
public final class DataBrokerFunction {
    private DataBrokerFunction() {
        throw new UnsupportedOperationException();
    }

    public static <T extends ServiceRef & ChildOf<Module>> T getRibInstance(final BGPConfigModuleProvider configModuleOp, final Function<String, T>
        function, final String instanceName, final ReadOnlyTransaction rTx) {
        Preconditions.checkNotNull(rTx);
        try {
            final Optional<Service> maybeService = configModuleOp.readConfigService(new ServiceKey(DomAsyncDataBroker.class), rTx);
            if (maybeService.isPresent()) {
                final Optional<Instance> maybeInstance = Iterables.tryFind(maybeService.get().getInstance(), new Predicate<Instance>() {
                    @Override
                    public boolean apply(final Instance instance) {
                        final String moduleName = OpenConfigUtil.getModuleName(instance.getProvider());
                        if (moduleName.equals(instanceName)) {
                            return true;
                        }
                        return false;
                    }
                });
                if (maybeInstance.isPresent()) {
                    return function.apply(maybeInstance.get().getName());
                }
            }
            return null;
        } catch (final ReadFailedException e) {
            throw new IllegalStateException("Failed to read service.", e);
        }
    }
}
