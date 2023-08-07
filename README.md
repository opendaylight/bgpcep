#### Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License v1.0 which accompanies this distribution,
and is available at http://www.eclipse.org/legal/epl-v10.html

# BGPCEP

BGPCEP project is an effort to bring two south-bound plugins into the controller:
one for supporting BGP Linkstate Distribution as a source of L3 topology information,
the other one to add support for Path Computation Element Protocol as a way to instantiate paths
into the underlying network.

## DIRECTORY ORGANIZATION

* concepts:
    * Common networking concepts, shared between the protocols

* util:
    * Common utility classes

* bgp:
    * BGP-related artifacts

* pcep:
    * PCEP-related artifacts

* rsvp:
    * RSVP modeling concepts (needed by pcep)

## HOW TO BUILD

In order to build it's required to have JDK 1.7+ and Maven 3+, to get
a build going it's needed to:

1. Go in the root directory and run
   `mvn clean install`

2. After successful completion, look for org.opendaylight.bgpcep artifacts in your local maven repository.
