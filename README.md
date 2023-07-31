[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opendaylight.bgpcep/bgpcep-artifacts/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.opendaylight.bgpcep/bgpcep-artifacts)
[![Javadocs](https://javadoc.io/badge2/org.opendaylight.bgpcep/bgpcep-karaf/javadoc.svg)](https://www.javadoc.io/doc/org.opendaylight.bgpcep)
[![License](https://img.shields.io/badge/License-EPL%201.0-blue.svg)](https://opensource.org/licenses/EPL-1.0)

[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=opendaylight_bgpcep&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=opendaylight_bgpcep)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=opendaylight_bgpcep&metric=alert_status)](https://sonarcloud.io/summary/overall?id=opendaylight_bgpcep)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=opendaylight_bgpcep&metric=sqale_index)](https://sonarcloud.io/summary/overall?id=opendaylight_bgpcep)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=opendaylight_bgpcep&metric=coverage)](https://sonarcloud.io/summary/overall?id=opendaylight_bgpcep)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=opendaylight_bgpcep&metric=ncloc)](https://sonarcloud.io/summary/overall?id=opendaylight_bgpcep)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=opendaylight_bgpcep&metric=code_smells)](https://sonarcloud.io/summary/overall?id=opendaylight_bgpcep)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=opendaylight_bgpcep&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=opendaylight_bgpcep)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=opendaylight_bgpcep&metric=security_rating)](https://sonarcloud.io/summary/overall?id=opendaylight_bgpcep)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=opendaylight_bgpcep&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=opendaylight_bgpcep)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=opendaylight_bgpcep&metric=duplicated_lines_density)](https://sonarcloud.io/summary/overall?id=opendaylight_bgpcep)

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
