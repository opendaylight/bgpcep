[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opendaylight.bgpcep/bgpcep-artifacts/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.opendaylight.bgpcep/bgpcep-artifacts)
[![License](https://img.shields.io/badge/License-EPL%201.0-blue.svg)](https://opensource.org/licenses/EPL-1.0)

# BGPCEP

BGPCEP project is an effort to bring two south-bound plugins into the controller:
one for supporting BGP Linkstate Distribution as a source of L3 topology information,
the other one to add support for Path Computation Element Protocol as a way to instantiate paths
into the underlying network.

## DIRECTORY ORGANIZATION

* src/site:
    * maven site plugin templates

* mockito-configuration:
    * default configuration of mockito framework
 
* concepts:
   *  Common networking concepts, shared between the protocols

* util:
    * Common utility classes

* bgp:
    * BGP-related artifacts

* pcep:
    * PCEP-related artifacts

* pcep/pcepy:                                                                                                                                                                                                                                                              
    * Python PCEP library                                                                                                                                                                                                                                                  
                                                                                                                                                                                                                                                                           
* pcep/pcepdump:                                                                                                                                                                                                                                                           
    * Python-based PCEP listener and debug tool                                                                                                                                                                                                                            
                                                                                                                                                                                                                                                                           
* rsvp:                                                                                                                                                                                                                                                                    
    * RSVP modeling concepts (needed by pcep)                                                                                                                                                                                                                              
                                                                                                                                                                                                                                                                           
## HOW TO BUILD                                                                                                                                                                                                                                                            
In order to build it's required to have JDK 1.7+ and Maven 3+, to get                                                                                                                                                                                                      
a build going it's needed to:                                                                                                                                                                                                                                              
                                                                                                                                                                                                                                                                           
1. Go in the root directory and run                                                                                                                                                                                                                                        
                                                                                                                                                                                                                                                                           
    `mvn clean install`                                                                                                                                                                                                                                                    
                                                                                                                                                                                                                                                                           
2. After successful completion, look for org.opendaylight.bgpcep artifacts in your local maven repository.
