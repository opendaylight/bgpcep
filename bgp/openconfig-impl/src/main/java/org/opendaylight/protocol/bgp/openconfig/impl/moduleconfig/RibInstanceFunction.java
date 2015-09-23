/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.RibInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.ServiceRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.ServiceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.service.Instance;
import org.opendaylight.yangtools.yang.binding.ChildOf;

final class RibInstanceFunction<T extends ServiceRef & ChildOf<Module>> implements AsyncFunction<String, T> {

    private final ReadTransaction rTx;
    private final BGPConfigModuleProvider configModuleOp;
    private final Function<String, T> function;

    public RibInstanceFunction(final ReadTransaction rTx, final BGPConfigModuleProvider configModuleOp, final Function<String, T> function) {
        this.rTx = Preconditions.checkNotNull(rTx);
        this.configModuleOp = Preconditions.checkNotNull(configModuleOp);
        this.function = Preconditions.checkNotNull(function);
    }

    @Override
    public ListenableFuture<T> apply(final String instanceName) {
        final ListenableFuture<Optional<Service>> readFuture = configModuleOp.readConfigService(new ServiceKey(RibInstance.class), rTx);
        return Futures.transform(readFuture, new Function<Optional<Service>, T>() {
            @Override
            public T apply(final Optional<Service> maybeService) {
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
            }

        });
    }
}
