/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object.bnc;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bnc.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bnc.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;

public final class BNCUtil {
    private BNCUtil() {
        // Hidden on purpose
    }

    public static List<Subobject> toBncSubobject(final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml
        .ns.yang.pcep.types.rev181109.explicit.route.object.ero.Subobject> subobject) {
        return subobject.stream()
            .map(sob -> {
                final SubobjectType type = sob.getSubobjectType();
                Preconditions.checkArgument(type instanceof IpPrefixCase,
                    "Wrong instance of PCEPObject. Passed %s. Needed IpPrefixCase.", type.getClass());
                return new SubobjectBuilder().setIpPrefix(((IpPrefixCase) type).getIpPrefix().getIpPrefix())
                    .setLoose(sob.isLoose()).build();
            }).collect(Collectors.toList());
    }

    public static List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit
        .route.object.ero.Subobject> toIroSubject(final List<Subobject> subobject) {
        return subobject.stream()
            .map(sob -> {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit
                    .route.subobjects.subobject.type.ip.prefix._case.IpPrefix prefix = new IpPrefixBuilder()
                    .setIpPrefix(sob.getIpPrefix()).build();
                final IpPrefixCase subObjType = new IpPrefixCaseBuilder().setIpPrefix(prefix).build();
                return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                    .explicit.route.object.ero.SubobjectBuilder()
                    .setSubobjectType(subObjType)
                    .setLoose(sob.isLoose())
                    .build();
            }).collect(Collectors.toList());
    }
}
