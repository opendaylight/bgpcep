.. _bgp-user-guide-rib-config-policies:

RIB Policy Configuration
========================

The OpenDaylight BGP implementation supports configurable RIB policies that allow the modification of import and export policies.

.. note:: Default ODL BGP RIB Config Policy is provided. Default policy is compliant with default behaviour from `RFC8212 <https://tools.ietf.org/html/rfc8212>`_. Any config policy to be used by Protocol must be configured and present before than Protocol configuration is added. If policy is reconfigured, protocol must be re configured again.

**URL:** ``/restconf/config/openconfig-routing-policy:routing-policy``

**RFC8040 URL:** ``/rests/data/openconfig-routing-policy:routing-policy?content=config``

**Method:** ``GET``

.. tabs::

   .. tab:: XML

      **Content-Type:** ``application/xml``

      **Request Body:**

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 2,15

         <routing-policy xmlns="http://openconfig.net/yang/routing-policy">
             <defined-sets>
                 <bgp-defined-sets xmlns="http://openconfig.net/yang/bgp-policy">
                     <cluster-id-sets xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy">
                         ...
                     </cluster-id-sets>
                     <role-sets xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy">
                         ...
                     </role-sets>
                     <originator-id-sets xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy">
                         ...
                     </originator-id-sets>
                 </bgp-defined-sets>
             </defined-sets>
             <policy-definitions>
                 <policy-definition>
                     <name>default-odl-export-policy</name>
                     <statements>
                         <statement>
                             <name>to-odl-internal</name>
                             <actions>
                                 <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
                                     ...
                                 </bgp-actions>
                             </actions>
                             <conditions>
                                 <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                                     ...
                                 </bgp-conditions>
                             </conditions>
                         </statement>
                         ...
                     </statements>
                 </policy-definition>
                 <policy-definition>
                     <name>default-odl-import-policy</name>
                     ...
                 </policy-definition>
             </policy-definitions>
         </routing-policy>

      @line 2: BGP defined sets.

      @line 15: Policy definitions.

   .. tab:: JSON

      **Content-Type:** ``application/json``

      **Request Body:**

      .. code-block:: json
         :linenos:
         :emphasize-lines: 3,10

         {
             "routing-policy": {
                 "defined-sets": {
                     "bgp-defined-sets": {
                         "cluster-id-sets": "...",
                         "role-sets": "...",
                         "originator-id-sets": "..."
                     }
                 },
                 "policy-definitions": {
                     "policy-definition": [
                         {
                             "name": "default-odl-export-policy",
                             "statements": {
                                 "statement": {
                                     "name": "to-odl-internal",
                                     "actions": {
                                         "bgp-actions": "..."
                                     },
                                     "conditions": {
                                         "bgp-conditions": "..."
                                     }
                                 },
                                 "#text": "..."
                             }
                         },
                         {
                             "name": "default-odl-import-policy",
                             "#text": "..."
                         }
                     ]
                 }
             }
         }

      @line 3: BGP defined sets.

      @line 10: Policy definitions.


Policy Configuration
--------------------

Conditions may include multiple match or comparison operations; similarly, actions may consist of a multitude of changes to route attributes or a final disposition regarding the acceptance or rejection of the route.

**URL:** ``/restconf/config/openconfig-routing-policy:routing-policy/openconfig-routing-policy:policy-definitions/``

**RFC8040 URL:** ``/rests/data/openconfig-routing-policy:routing-policy/openconfig-routing-policy:policy-definitions``

**Method:** ``POST``

.. tabs::

   .. tab:: XML

      **Content-Type:** ``application/xml``

      **Request Body:**

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 2,5,7,10

         <policy-definition xmlns="http://openconfig.net/yang/routing-policy">
             <name>odl-policy-example</name>
             <statements>
                 <statement>
                     <name>reject-all-incoming-routes</name>
                     <actions>
                         <reject-route/>
                     </actions>
                     <conditions>
                         <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                             <match-role-set xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy">
                                 <from-role>
                                    <role-set>/rpol:routing-policy/rpol:defined-sets/bgppol:bgp-defined-sets/role-sets/role-set[role-set-name="all"]</role-set>
                                 </from-role>
                             </match-role-set>
                         </bgp-conditions>
                     </conditions>
                 </statement>
             </statements>
         </policy-definition>

      @line 2: The unique policy instance identifier.

      @line 5: Policy Statement Identifier.

      @line 7: Actions.

      @line 10: BGP Conditions.

   .. tab:: JSON

      **Content-Type:** ``application/json``

      **Request Body:**

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,8,10,15

         {
             "policy-definition": [
                 {
                     "name": "odl-policy-example",
                     "statements": {
                         "statement": [
                             {
                                 "name": "reject-all-incoming-routes",
                                 "actions": {
                                     "reject-route": [
                                         null
                                     ]
                                 },
                                 "conditions": {
                                     "openconfig-bgp-policy:bgp-conditions": {
                                         "odl-bgp-policy:match-role-set": {
                                             "from-role": {
                                                 "role-set": "/rpol:routing-policy/rpol:defined-sets/bgppol:bgp-defined-sets/role-sets/role-set[role-set-name=\"all\"]"
                                             }
                                         }
                                     }
                                 }
                             }
                         ]
                     }
                 }
             ]
         }

      @line 4: The unique policy instance identifier.

      @line 8: Policy Statement Identifier.

      @line 10: Actions.

      @line 15: BGP Conditions.

-----

The new instance presence can be verified via REST:

**URL:** ``/restconf/config/openconfig-routing-policy:routing-policy/openconfig-routing-policy:policy-definitions/policy-definition/odl-policy-example``

**RFC8040 URL:** ``/rests/data/openconfig-routing-policy:routing-policy/openconfig-routing-policy:policy-definitions/policy-definition=odl-policy-example``

**Method:** ``GET``

.. tabs::

   .. tab:: XML

      **Response Body:**

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 2,5

         <policy-definition xmlns="http://openconfig.net/yang/routing-policy">
             <name>odl-policy-example</name>
             <statements>
                 <statement>
                     <name>reject-all-incoming-routes</name>
                     <actions>
                         <reject-route></reject-route>
                     </actions>
                     <conditions>
                         <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                             <match-role-set xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy">
                                 <from-role>
                                     <role-set>/rpol:routing-policy/rpol:defined-sets/bgppol:bgp-defined-sets/role-sets/role-set[role-set-name="all"]</role-set>
                                     <match-set-options>ANY</match-set-options>
                                 </from-role>
                             </match-role-set>
                         </bgp-conditions>
                     </conditions>
                 </statement>
             </statements>
         </policy-definition>

      @line 2: Policy definition Identifier.

      @line 5: Policy Statement Identifier.

   .. tab:: JSON

      **Response Body:**

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,8

         {
             "policy-definition": [
                 {
                     "name": "odl-policy-example",
                     "statements": {
                         "statement": [
                             {
                                 "name": "reject-all-incoming-routes",
                                 "actions": {
                                     "reject-route": [
                                         null
                                     ]
                                 },
                                 "conditions": {
                                     "openconfig-bgp-policy:bgp-conditions": {
                                         "odl-bgp-policy:match-role-set": {
                                             "from-role": {
                                                 "role-set": "/rpol:routing-policy/rpol:defined-sets/bgppol:bgp-defined-sets/role-sets/role-set[role-set-name=\"all\"]"
                                             }
                                         }
                                     }
                                 }
                             }
                         ]
                     }
                 }
             ]
         }

      @line 4: Policy definition Identifier.

      @line 8: Policy Statement Identifier.

Actions
```````
ODL BGP by default provides support for a group of BGP Actions.

Accept
''''''
Default policy to accept the route.

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 2

         <actions>
             <accept-route/>
         </actions>

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 2

         {
             "actions": {
                 "accept-route": {
                 }
             }
         }

Reject
''''''
Default policy to reject the route.

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 2

         <actions>
            <reject-route/>
         </actions>

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 2

         {
             "actions": {
                 "reject-route" : {
                 }
             }
         }

As-path prepend
'''''''''''''''
Action to prepend local AS number to the AS-path

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <actions>
            <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
                <set-as-path-prepend/>
            </bgp-actions>
         </actions>

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 2

         {
             "actions": {
                 "bgp-actions" : {
                     "set-as-path-prepend": {
                     }
                 }
             }
         }

Originator Id prepend
'''''''''''''''''''''''''
Action to prepend Originator Id. In case there is non Originator Id present, local Originator Id is prepend.

* Local

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 2

         <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
            <set-originator-id-prepend xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy"/>
         </bgp-actions>

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 2

         {
             "bgp-actions" : {
                 "set-originator-id-prepend": {
                 }
             }
         }

* By value

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 2

         <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
             <set-originator-id-prepend xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy">
                 <originator-id>192.0.2.1</originator-id>
             </set-originator-id-prepend>
         </bgp-actions>

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 2

         {
             "bgp-actions" : {
                 "set-originator-id-prepend": {
                     "originator-id": "192.0.2.1"
                 }
             }
         }

Cluster Id prepend
''''''''''''''''''
Action to prepend local Cluster Id to Cluster Id List.

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <actions>
             <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
                 <set-cluster-id-prepend xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy"/>
             </bgp-actions>
         </actions>

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 3

         {
             "actions": {
                 "bgp-actions" : {
                     "set-cluster-id-prepend": {
                     }
                 }
             }
         }

Set Route Origin
''''''''''''''''
Set the origin attribute to the specified value.

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <actions>
             <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
                 <set-route-origin>IGP</set-route-origin>
             </bgp-actions>
         </actions>

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 3

         {
             "actions": {
                 "bgp-actions" : {
                     "set-route-origin": "IGP"
                 }
             }
         }

Set Local Preference
''''''''''''''''''''
Set the local pref attribute on the route update.

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <actions>
             <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
                 <set-local-pref>100</set-local-pref>
             </bgp-actions>
         </actions>

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 3

         {
             "actions": {
                 "bgp-actions" : {
                     "set-local-pref": 100
                 }
             }
         }

Set NextHop
'''''''''''
Set the next-hop attribute in the route update.

* Local

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <actions>
             <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
                 <set-next-hop>SELF</set-next-hop>
             </bgp-actions>
         </actions>

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 3

         {
             "actions": {
                 "bgp-actions" : {
                     "set-next-hop": "SELF"
                 }
             }
         }

* By value

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <actions>
             <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
                 <set-next-hop>4.5.6.7</set-next-hop>
             </bgp-actions>
         </actions>

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 3

         {
             "actions": {
                 "bgp-actions" : {
                     "set-next-hop": "4.5.6.7"
                 }
             }
         }

Set MED
'''''''
Set the med metric attribute in the route update.

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <actions>
             <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
                 <set-med>15</set-med>
             </bgp-actions>
         </actions>

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 3

         {
             "actions": {
                 "bgp-actions" : {
                     "set-med": 15
                 }
             }
         }

Community set prepend
'''''''''''''''''''''
Action to set the community attributes of the route, along with options to modify how the community is modified.

* Inline

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <actions>
             <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
                 <set-community>
                     <communities>
                         <as-number>65</as-number>
                         <semantics>10</semantics>
                     </communities>
                     <communities>
                         <as-number>66</as-number>
                         <semantics>11</semantics>
                     </communities>
                     <options>ADD</options>
                 </set-community>
             </bgp-actions>
         </actions>

      @line 3: Set Community.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4

         {
             "actions": {
                 "bgp-actions" : {
                     "set-community": {
                         "communities": [
                             {
                                 "as-number": 65,
                                 "semantics": 10
                             },
                             {
                                 "as-number": 66,
                                 "semantics": 11
                             }
                         ],
                         "options": "ADD"
                     }
                 }
             }
         }

      @line 4: Set Community.

* By reference

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,5,7

         <actions>
             <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
                 <set-community>
                     <community-set-ref>
                         /rpol:routing-policy/rpol:defined-sets/rpol:community-sets/community-set[community-set-name="community-set-name-example"]
                     </community-set-ref>
                     <options>ADD</options>
                 </set-community>
             </bgp-actions>
         </actions>

      @line 3: Set Community.

      @line 5: Community set reference.

      @line 7: Options are ADD, REMOVE, REPLACE.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,5,6

         {
             "actions": {
                 "bgp-actions" : {
                     "set-community": {
                        "community-set-ref": "/rpol:routing-policy/rpol:defined-sets/rpol:community-sets/community-set[community-set-name=\"community-set-name-example\"]",
                         "options": "ADD"
                     }
                 }
             }
         }

      @line 4: Set Community.

      @line 5: Community set reference.

      @line 6: Options are ADD, REMOVE, REPLACE.

-----

Defined set

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <defined-sets>
             <bgp-defined-sets xmlns="http://openconfig.net/yang/bgp-policy">
                 <community-sets>
                     <community-set>
                         <community-set-name>community-set-name-test</community-set-name>
                         <communities>
                             <as-number>65</as-number>
                             <semantics>10</semantics>
                         </communities>
                         <communities>
                             <as-number>66</as-number>
                             <semantics>11</semantics>
                         </communities>
                     </community-set>
                 </community-sets>
             </bgp-defined-sets>
         </defined-sets>

      @line 3: Community set.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4

         {
             "defined-sets": {
                 "bgp-defined-sets" : {
                     "community-sets": {
                         "community-set": {
                             "community-set-name": "community-set-name-test",
                             "communities": [
                                 {
                                     "as-number": 65,
                                     "semantics": 10
                                 },
                                 {
                                     "as-number": 66,
                                     "semantics": 11
                                 }
                             ]
                         }
                     }
                 }
             }
         }

      @line 4: Set Community.

Extended Community set action
''''''''''''''''''''''''''''''
Action to set the extended community attributes of the route, along with options to modify how the community is modified.

* Inline

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <actions>
             <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
                 <set-ext-community>
                     <ext-community-member>
                         <encapsulation-extended-community>
                             <tunnel-type>vxlan</tunnel-type>
                         </encapsulation-extended-community>
                     </ext-community-member>
                     <ext-community-member>
                         <as-4-route-origin-extended-community>
                             <as-4-specific-common>
                                 <as-number>65000</as-number>
                                 <local-administrator>123</local-administrator>
                             </as-4-specific-common>
                         </as-4-route-origin-extended-community>
                     </ext-community-member>
                     <options>ADD</options>
                 </set-ext-community>
             </bgp-actions>
         </actions>

      @line 3: Set Extended Community.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4

         {
             "actions": {
                 "bgp-actions": {
                     "set-ext-community": {
                         "ext-community-member": [
                             {
                                 "encapsulation-extended-community": {
                                     "tunnel-type": "vxlan"
                                 }
                             },
                             {
                                 "as-4-route-origin-extended-community": {
                                     "as-4-specific-common": {
                                         "as-number": "65000",
                                         "local-administrator": "123"
                                     }
                                 }
                             }
                         ],
                         "options": "ADD"
                     }
                 }
             }
         }

      @line 4: Set Extended Community.

* By reference

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,5,7

         <actions>
             <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
                 <set-ext-community>
                     <ext-community-set-ref>
                         /rpol:routing-policy/rpol:defined-sets/rpol:ext-community-sets/ext-community-set[ext-community-set-name="ext-community-set-name-example"]
                     </ext-community-set-ref>
                     <options>REMOVE</options>
                 </set-ext-community>
             </bgp-actions>
         </actions>

      @line 3: Set Extended Community.

      @line 5: Extended Community set reference.

      @line 7: Options are ADD, REMOVE, REPLACE.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,5,6

         {
             "actions": {
                 "bgp-actions" : {
                     "set-ext-community": {
                        "ext-community-set-ref": "/rpol:routing-policy/rpol:defined-sets/rpol:community-sets/community-set[community-set-name=\"community-set-name-example\"]",
                         "options": "REMOVE"
                     }
                 }
             }
         }

      @line 4: Set Extended Community.

      @line 5: Extended Community set reference.

      @line 6: Options are ADD, REMOVE, REPLACE.

-----

Defined set

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,5

         <defined-sets>
             <bgp-defined-sets xmlns="http://openconfig.net/yang/bgp-policy">
                 <ext-community-sets>
                     <ext-community-set>
                         <ext-community-set-name>ext-community-set-name-test</ext-community-set-name>
                         <ext-community-member>
                             <encapsulation-extended-community>
                                 <tunnel-type>vxlan</tunnel-type>
                             </encapsulation-extended-community>
                         </ext-community-member>
                         <ext-community-member>
                             <as-4-route-origin-extended-community>
                                 <as-4-specific-common>
                                     <as-number>65000</as-number>
                                     <local-administrator>123</local-administrator>
                                 </as-4-specific-common>
                             </as-4-route-origin-extended-community>
                         </ext-community-member>
                     </ext-community-set>
                 </ext-community-sets>
             </bgp-defined-sets>
         </defined-sets>

      @line 3: Extendend Community set.

      @line 5: Extendend Community set name.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,5

         {
             "defined-sets": {
                 "bgp-defined-sets" : {
                     "ext-community-sets": {
                         "ext-community-set": {
                             "ext-community-set-name": "ext-community-set-name-test",
                             "ext-community-member": [
                                 {
                                     "encapsulation-extended-community": {
                                         "tunnel-type": "vxlan"
                                     },
                                     "as-4-route-origin-extended-community": {
                                         "as-4-specific-common": {
                                             "as-number": 65000,
                                             "local-administrator": 123
                                         }
                                     }
                                 }
                             ]
                         }
                     }
                 }
             }
         }

      @line 4: Extendend Community set.

      @line 5: Extendend Community set name.

Filter Non transitive attributes
''''''''''''''''''''''''''''''''
Filters attributes, removing non transitive attributes.

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <actions>
             <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
                 <non-transitive-attributes-filter xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy"/>
             </bgp-actions>
         </actions>

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4

         {
             "actions": {
                 "bgp-actions" : {
                     "non-transitive-attributes-filter": {
                     }
                 }
             }
         }

Client Attribute Prepend
''''''''''''''''''''''''
Replace attributes per any VPN Route attributes from client Peer, if present.

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <actions>
             <bgp-actions xmlns="http://openconfig.net/yang/bgp-policy">
                 <client-attribute-prepend xmlns="urn:opendaylight:params:xml:ns:yang:bgp:route:target:constrain"/>
             </bgp-actions>
         </actions>

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4

         {
             "actions": {
                 "bgp-actions" : {
                     "client-attribute-prepend": {
                     }
                 }
             }
         }

Conditions
``````````
ODL BGP by default provides support for a group of BGP Conditions.

Match BGP Neighbor Set
''''''''''''''''''''''

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,4,5,6

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <match-bgp-neighbor-set xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy">
                     <from-neighbor>
                         <neighbor-set>/rpol:routing-policy/rpol:defined-sets/rpol:neighbor-sets/neighbor-set[neighbor-set-name="bgp-neighbor-set-example"]</neighbor-set>
                         <match-set-options>INVERT</match-set-options>
                     </from-neighbor>
                 </match-bgp-neighbor-set>
             </bgp-conditions>
         </conditions>

      @line 3: Match BGP Neighbor Condition set.

      @line 4: Match BGP Neighbor from whom we receive the route.

      @line 5: Match BGP Neighbor Set reference.

      @line 6: Match Set Options (ANY, INVERT)

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,5,6,7

         {
             "conditions": {
                 "bgp-conditions" : {
                     "match-bgp-neighbor-set": {
                         "from-neighbor": {
                             "neighbor-set": "/rpol:routing-policy/rpol:defined-sets/rpol:neighbor-sets/neighbor-set[neighbor-set-name=\"bgp-neighbor-set-example\"]",
                             "match-set-options": "INVERT"
                         }
                     }
                 }
             }
         }

      @line 4: Match BGP Neighbor Condition set.

      @line 5: Match BGP Neighbor from whom we receive the route.

      @line 6: Match BGP Neighbor Set reference.

      @line 7: Match Set Options (ANY, INVERT)

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,4,5,6

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <match-bgp-neighbor-set xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy">
                     <to-neighbor>
                         <neighbor-set>/rpol:routing-policy/rpol:defined-sets/rpol:neighbor-sets/neighbor-set[neighbor-set-name="bgp-neighbor-set-example"]</neighbor-set>
                         <match-set-options>INVERT</match-set-options>
                     </to-neighbor>
                 </match-bgp-neighbor-set>
             </bgp-conditions>
         </conditions>

     @line 3: Match BGP Neighbor Condition set.

     @line 4: Match BGP Neighbor to whom we send the route.

     @line 5: Match BGP Neighbor Set reference.

     @line 6: Match Set Options (ANY, INVERT)

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,5,6,7

         {
             "conditions": {
                 "bgp-conditions" : {
                     "match-bgp-neighbor-set": {
                         "to-neighbor": {
                             "neighbor-set": "/rpol:routing-policy/rpol:defined-sets/rpol:neighbor-sets/neighbor-set[neighbor-set-name=\"bgp-neighbor-set-example\"]",
                             "match-set-options": "INVERT"
                         }
                     }
                 }
             }
         }

      @line 4: Match BGP Neighbor Condition set.

      @line 5: Match BGP Neighbor to whom we receive the route.

      @line 6: Match BGP Neighbor Set reference.

      @line 7: Match Set Options (ANY, INVERT)

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,4,5,7,8,9

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <match-bgp-neighbor-set xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy">
                     <from-neighbor>
                         <neighbor-set>/rpol:routing-policy/rpol:defined-sets/rpol:neighbor-sets/neighbor-set[neighbor-set-name="bgp-neighbor-set-example"]</neighbor-set>
                     </from-neighbor>
                     <to-neighbor>
                         <neighbor-set>/rpol:routing-policy/rpol:defined-sets/rpol:neighbor-sets/neighbor-set[neighbor-set-name="bgp-neighbor-set-example"]</neighbor-set>
                         <match-set-options>INVERT</match-set-options>
                     </to-neighbor>
                 </match-bgp-neighbor-set>
             </bgp-conditions>
         </conditions>

      @line 3: Match BGP Neighbor Condition set.

      @line 4: Match BGP Neighbor from whom we receive the route.

      @line 5: Match BGP Neighbor Set reference.

      @line 7: Match BGP Neighbor to whom we send the route.

      @line 8: Match BGP Neighbor Set reference.

      @line 9: Match Set Options (ANY, INVERT)

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,5,6,8,9,10

         {
             "conditions": {
                 "bgp-conditions" : {
                     "match-bgp-neighbor-set": {
                         "from-neighbor": {
                             "neighbor-set": "/rpol:routing-policy/rpol:defined-sets/rpol:neighbor-sets/neighbor-set[neighbor-set-name=\"bgp-neighbor-set-example\"]",
                         },
                         "to-neighbor": {
                             "neighbor-set": "/rpol:routing-policy/rpol:defined-sets/rpol:neighbor-sets/neighbor-set[neighbor-set-name=\"bgp-neighbor-set-example\"]",
                             "match-set-options": "INVERT"
                         }
                     }
                 }
             }
         }

      @line 4: Match BGP Neighbor Condition set.

      @line 5: Match BGP Neighbor from whom we receive the route.

      @line 6: Match BGP Neighbor Set reference.

      @line 8: Match BGP Neighbor to whom we send the route.

      @line 9: Match BGP Neighbor Set reference.

      @line 10: Match Set Options (ANY, INVERT)

-----

Defined set

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,5

         <defined-sets>
             <neighbor-sets>
                 <neighbor-set>
                     <neighbor-set-name>bgp-neighbor-set-example</neighbor-set-name>
                     <neighbor>
                         <address>127.0.0.1</address>
                     </neighbor>
                     <neighbor>
                         <address>127.0.0.2</address>
                     </neighbor>
                 </neighbor-set>
             </neighbor-sets>
         </defined-sets>

      @line 3: Originator Id Set.

      @line 5: Originator Id Set name.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,5

         {
             "defined-sets": {
                 "neighbor-sets": {
                     "neighbor-set": {
                         "neighbor-set-name": "bgp-neighbor-set-example",
                          "neighbor": [
                              {
                                  "address": "127.0.0.1"
                              },
                              {
                                  "address": "127.0.0.2"
                              }
                          ]
                     }
                 }
             }
         }

      @line 4: Originator Id Set.

      @line 5: Originator Id Set name.

Match Originator Id Set
'''''''''''''''''''''''

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,5,7

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <match-originator-id-set-condition xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy">
                     <originator-id-set>
                         /rpol:routing-policy/rpol:defined-sets/bgppol:bgp-defined-sets/originator-id-sets/originator-id-set[originator-set-name="local-originator-id"]
                     </originator-id-set>
                     <match-set-options>INVERT</match-set-options>
                 </match-originator-id-set-condition>
             </bgp-conditions>
         </conditions>

      @line 3: Match Originator Id Condition set.

      @line 5: Match Originator Id Set reference.

      @line 7: Match Set Options (ANY, INVERT)

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,5,6

         {
             "conditions": {
                 "bgp-conditions" : {
                     "match-originator-id-set-condition": {
                         "originator-id-set": "/rpol:routing-policy/rpol:defined-sets/bgppol:bgp-defined-sets/originator-id-sets/originator-id-set[originator-set-name=\"local-originator-id\"]",
                         "match-set-options": "INVERT"
                     }
                 }
             }
         }

      @line 4: Match Originator Id Condition set.

      @line 5: Match Originator Id Set reference.

      @line 6: Match Set Options (ANY, INVERT)

-----

Defined set

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,5

         <defined-sets>
             <bgp-defined-sets xmlns="http://openconfig.net/yang/bgp-policy">
                 <originator-id-sets xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy">
                     <originator-id-set>
                         <originator-id-set-name>local-originator-id</originator-id-set-name>
                         <local/>
                     </originator-id-set>
                 </originator-id-sets>
             </bgp-defined-sets>
         </defined-sets>

      @line 3: Originator Id Set.

      @line 5: Originator Id Set name.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,5

         {
             "defined-sets": {
                 "bgp-defined-sets" : {
                     "originator-id-sets": {
                         "originator-id-set": {
                             "originator-id-set-name": "local-originator-id"
                         }
                     }
                 }
             }
         }

      @line 4: Originator Id Set.

      @line 5: Originator Id Set name.

Match Cluster Id Set
''''''''''''''''''''

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,5

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <match-cluster-id-set-condition xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy">
                     <cluster-id-set>
                         /rpol:routing-policy/rpol:defined-sets/bgppol:bgp-defined-sets/cluster-id-sets/cluster-id-set[cluster-set-name="local-cluster-id"]
                     </cluster-id-set>
                     <match-set-options>INVERT</match-set-options>
                 </match-cluster-id-set-condition>
             </bgp-conditions>
         </conditions>

      @line 3: Match Cluster Id Condition set.

      @line 5: Match Cluster Id Set reference.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,5

         {
             "conditions": {
                 "bgp-conditions" : {
                     "match-cluster-id-set-condition": {
                         "cluster-id-set": "/rpol:routing-policy/rpol:defined-sets/bgppol:bgp-defined-sets/cluster-id-sets/cluster-id-set[cluster-set-name=\"local-cluster-id\"]",
                         "match-set-options": "INVERT"
                     }
                 }
             }
         }

      @line 4: Match Cluster Id Condition set.

      @line 5: Match Cluster Id Set reference.

-----

Defined set

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,5

         <defined-sets>
             <bgp-defined-sets xmlns="http://openconfig.net/yang/bgp-policy">
                 <cluster-id-sets xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy">
                     <cluster-id-set>
                         <cluster-id-set-name>local-cluster-id</cluster-id-set-name>
                         <local/>
                     </cluster-id-set>
                 </cluster-id-sets>
             </bgp-defined-sets>
         </defined-sets>

      @line 3: Cluster Id Set.

      @line 5: Cluster Id Set name.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,5

         {
             "defined-sets": {
                 "bgp-defined-sets" : {
                     "cluster-id-sets": {
                         "cluster-id-set": {
                             "cluster-id-set-name": "local-cluster-id"
                         }
                     }
                 }
             }
         }

      @line 4: Cluster Id Set.

      @line 5: Cluster Id Set name.

Match Peer Role Set
'''''''''''''''''''

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,5,6

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <match-role-set xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy">
                     <from-role>
                         <role-set>/rpol:routing-policy/rpol:defined-sets/bgppol:bgp-defined-sets/role-sets/role-set[role-set-name="only-ibgp"]</role-set>
                         <match-set-options>INVERT</match-set-options>
                     </from-role>
                     <to-role>
                         <role-set>/rpol:routing-policy/rpol:defined-sets/bgppol:bgp-defined-sets/role-sets/role-set[role-set-name="all"]</role-set>
                     <to-role>
                 </match-role-set>
             </bgp-conditions>
         </conditions>

      @line 3: Match Role Set.

      @line 5: Match Role Set reference.

      @line 6: Match Set Options (ANY, INVERT)

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,6,7

         {
             "conditions": {
                 "bgp-conditions" : {
                     "match-role-set": {
                         "from-role": {
                             "role-set": "/rpol:routing-policy/rpol:defined-sets/bgppol:bgp-defined-sets/role-sets/role-set[role-set-name=\"only-ibgp\"]"
                             "match-set-options": "INVERT"
                         },
                         "to-role": {
                             "role-set": "/rpol:routing-policy/rpol:defined-sets/bgppol:bgp-defined-sets/role-sets/role-set[role-set-name=\"all\"]"
                         }
                     }
                 }
             }
         }

      @line 4: Match Role Set.

      @line 6: Match Role Set reference.

      @line 7: Match Set Options (ANY, INVERT)

-----

Defined set

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,4,10,11

         <defined-sets>
             <bgp-defined-sets xmlns="http://openconfig.net/yang/bgp-policy">
                 <role-set>
                     <role-set-name>all</role-set-name>
                     <role>ebgp</role>
                     <role>ibgp</role>
                     <role>rr-client</role>
                     <role>internal</role>
                 </role-set>
                 <role-set>
                     <role-set-name>only-ibgp</role-set-name>
                     <role>ibgp</role>
                 </role-set>
             </bgp-defined-sets>
         </defined-sets>

      @line 3: Role Set.

      @line 4: Role Set name.

      @line 10: Role Set.

      @line 11: Role Id Set name.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,6,14,15

         {
             "defined-sets": {
                 "bgp-defined-sets" : {
                     "role-set": [
                         {
                             "role-set-name": "all",
                             "role": [
                                 "ebgp",
                                 "ibgp",
                                 "rr-client",
                                 "internal"
                             ]
                         },
                         {
                             "role-set-name": "only-ibgp",
                             "role": "ibgp"
                         }
                     ]
                 }
             }
         }

      @line 4: Role Set.

      @line 6: Role Set name.

      @line 14: Role Set.

      @line 15: Role Id Set name.

Match AS Path Set
'''''''''''''''''

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,5,7

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <match-as-path-set>
                     <as-path-set>
                         /rpol:routing-policy/rpol:defined-sets/bgp-pol:bgp-defined-sets/bgp-pol:as-path-sets/bgp-pol:as-path-set/[as-path-set-name="as-path-set-example"]
                     </as-path-set>
                     <match-set-options>ANY</match-set-options>
                 </match-as-path-set>
             </bgp-conditions>
         </conditions>

      @line 3: Match AS Path Set.

      @line 5: AS Path Set reference.

      @line 7: Match Set Option(ANY, ALL, INVERT).

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,6,7

         {
             "conditions": {
                 "bgp-conditions" : {
                     "match-as-path-set": {
                             "as-path-set": "/rpol:routing-policy/bgp-pol:bgp-defined-sets/bgp-pol:as-path-sets/bgp-pol:as-path-set/[as-path-set-name=\"as-path-set-example\"]"
                             "match-set-options": "INVERT"
                     }
                 }
             }
         }

      @line 4: Match AS Path Set.

      @line 6: AS Path Set reference.

      @line 7: Match Set Option(ANY, ALL, INVERT).

-----

Defined set

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 4,5,6

         <defined-sets>
             <bgp-defined-sets xmlns="http://openconfig.net/yang/bgp-policy">
                 <as-path-sets>
                     <as-path-set>
                         <as-path-set-name>as-path-set-example</as-path-set-name>
                         <as-path-set-member>65</as-path-set-member>
                         <as-path-set-member>64</as-path-set-member>
                         <as-path-set-member>63</as-path-set-member>
                     </as-path-set>
                 </as-path-sets>
             </bgp-defined-sets>
         </defined-sets>

      @line 4: AS Path Set.

      @line 5: AS Path Set name.

      @line 6: AS Path set member

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,5,6

         {
             "defined-sets": {
                 "bgp-defined-sets" : {
                     "as-path-sets": {
                         "as-path-set-name": "as-path-set-example",
                         "as-path-set-member": [
                              65,
                              64,
                              63
                         ]
                     }
                 }
             }
         }

      @line 4: AS Path Set.

      @line 5: AS Path Set name.

      @line 6: AS Path set member

Match Community Set
'''''''''''''''''''

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,5,7

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <match-community-set>
                     <community-set>
                         /rpol:routing-policy/rpol:defined-sets/rpol:community-sets/community-set[community-set-name="community-set-name-example"]
                     </community-set>
                     <match-set-options>ANY</match-set-options>
                 </match-community-set>
             </bgp-conditions>
         </conditions>

      @line 3: Match Community Set.

      @line 5: Match Community Set reference.

      @line 7: Match Set Option(ANY, ALL, INVERT).

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,6,7

         {
             "conditions": {
                 "bgp-conditions" : {
                     "match-community-set": {
                             "community-set": "/rpol:routing-policy/rpol:bgp-defined-sets/rpol:community-sets/community-set[community-set-name=\"community-set-name-example\"]"
                             "match-set-options": "ANY"
                     }
                 }
             }
         }

      @line 4: Match Community Set.

      @line 6: Match Community Set reference.

      @line 7: Match Set Option(ANY, ALL, INVERT).

-----

Defined set

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 4,5,6,10

         <defined-sets>
             <bgp-defined-sets xmlns="http://openconfig.net/yang/bgp-policy">
                 <community-sets>
                     <community-set>
                         <community-set-name>community-set-name-example</community-set-name>
                         <communities>
                             <as-number>65</as-number>
                             <semantics>10</semantics>
                         </communities>
                         <communities>
                             <as-number>66</as-number>
                             <semantics>11</semantics>
                         </communities>
                     </community-set>
                 </community-sets>
             </bgp-defined-sets>
         </defined-sets>

      @line 4: Community Set.

      @line 5: Community Set name.

      @line 6: Communities.

      @line 10: Communities.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 5,6,7,12

         {
             "defined-sets": {
                 "bgp-defined-sets" : {
                     "community-sets": {
                         "community-set": {
                             "community-set-name": "community-set-name-example",
                             "communities": [
                                 {
                                     "as-number": "65",
                                     "semantics": "10"
                                 },
                                 {
                                     "as-number": "66",
                                     "semantics": "11"
                                 }
                             ]
                         }
                     }
                 }
             }
         }

      @line 5: Community Set.

      @line 6: Community Set name.

      @line 7: Communities.

      @line 12: Communities.

Match Extended Community Set
''''''''''''''''''''''''''''
.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,5,7

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <match-ext-community-set>
                     <ext-community-set>
                         /rpol:routing-policy/rpol:defined-sets/rpol:ext-community-sets/ext-community-set[ext-community-set-name="ext-community-set-name-test"]
                     </ext-community-set>
                     <match-set-options>ANY</match-set-options>
                 </match-ext-community-set>
             </bgp-conditions>
         </conditions>

      @line 3: Match Extended Community Set.

      @line 5: Match Extended Community Set reference.

      @line 7: Match Set Option(ANY, ALL, INVERT).

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,6,7

         {
             "conditions": {
                 "bgp-conditions" : {
                     "match-ext-community-set": {
                             "ext-community-set": "/rpol:routing-policy/rpol:bgp-defined-sets/rpol:ext-community-sets/ext-community-set[ext-community-set-name=\"ext-community-set-name-test\"]"
                             "match-set-options": "ANY"
                     }
                 }
             }
         }

      @line 4: Match Extended Community Set.

      @line 6: Match Extended Community Set reference.

      @line 7: Match Set Option(ANY, ALL, INVERT).

-----

Defined set

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 4,5,6,11

         <defined-sets>
             <bgp-defined-sets xmlns="http://openconfig.net/yang/bgp-policy">
                 <ext-community-sets>
                     <ext-community-set>
                         <ext-community-set-name>ext-community-set-name-test</ext-community-set-name>
                         <ext-community-member>
                             <encapsulation-extended-community>
                                 <tunnel-type>vxlan</tunnel-type>
                             </encapsulation-extended-community>
                         </ext-community-member>
                         <ext-community-member>
                             <as-4-route-origin-extended-community>
                                 <as-4-specific-common>
                                     <as-number>65000</as-number>
                                     <local-administrator>123</local-administrator>
                                 </as-4-specific-common>
                             </as-4-route-origin-extended-community>
                         </ext-community-member>
                     </ext-community-set>
                 </ext-community-sets>
             </bgp-defined-sets>
         </defined-sets>

      @line 4: Extended Community Set.

      @line 5: Extended Community Set name.

      @line 6: Extended Communities.

      @line 11: Extended Communities.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 5,6,7,12

         {
             "defined-sets": {
                 "bgp-defined-sets" : {
                     "ext-community-sets": {
                         "ext-community-set": {
                             "ext-community-set-name": "ext-community-set-name-test",
                             "ext-community-member": [
                                 {
                                     "encapsulation-extended-community": {
                                         "tunnel-type": "vxlan"
                                     },
                                     "as-4-route-origin-extended-community": {
                                         "as-4-specific-common": {
                                             "as-number": 65000,
                                             "local-administrator": 123
                                         }
                                     }
                                 }
                             ]
                         }
                     }
                 }
             }
         }

      @line 5: Extended Community Set.

      @line 6: Extended Community Set name.

      @line 7: Extended Communities.

      @line 12: Extended Communities.

Match in Afi Safi
'''''''''''''''''
.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <afi-safi-in xmlns:x="http://openconfig.net/yang/bgp-types">x:IPV4-UNICAST</afi-safi-in>
             </bgp-conditions>
         </conditions>

      @line 3: Afi Safi match.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4

         {
             "conditions": {
                 "bgp-conditions" : {
                     "afi-safi-in": "x:IPV4-UNICAST"
                 }
             }
         }

      @line 4: Afi Safi match.

Match not in Afi Safi
'''''''''''''''''''''
.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <afi-safi-not-in xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy"
                 xmlns:x="http://openconfig.net/yang/bgp-types">x:IPV4-UNICAST</afi-safi-not-in>
                 <afi-safi-not-in xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy"
                 xmlns:x="http://openconfig.net/yang/bgp-types">x:IPV6-UNICAST</afi-safi-not-in>
             </bgp-conditions>
         </conditions>

      @line 3: Afi Safi not in match.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4

         {
             "conditions": {
                 "bgp-conditions" : {
                     "afi-safi-not-in": [
                         "x:IPV4-UNICAST",
                         "x:IPV6-UNICAST"
                     ]
                 }
             }
         }

      @line 4: Afi Safi not in match.

Match As Path Length
''''''''''''''''''''
.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <as-path-length>
                     <operator xmlns:x="http://openconfig.net/yang/policy-types">x:attribute-eq</operator>
                     <value>2</value>
                 </as-path-length>
             </bgp-conditions>
         </conditions>

      @line 3: As Path Length match.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4

         {
             "conditions": {
                 "bgp-conditions" : {
                     "as-path-length": {
                         "operator": "x:attribute-eq",
                         "value": 2
                     }
                 }
             }
         }

      @line 4: As Path Length match.

Match Local Pref
''''''''''''''''
.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <local-pref-eq>100</local-pref-eq>
             </bgp-conditions>
         </conditions>

      @line 3: Local Preference match.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4

         {
             "conditions": {
                 "bgp-conditions" : {
                     "local-pref-eq": 100
                 }
             }
         }

      @line 4: Local Preference match.

Match Origin
''''''''''''
.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <origin-eq>IGP</origin-eq>
             </bgp-conditions>
         </conditions>

      @line 3: Origin match.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4

         {
             "conditions": {
                 "bgp-conditions" : {
                     "origin-eq": "IGP"
                 }
             }
         }

      @line 4: Origin match.

Match MED
'''''''''
.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <med-eq>100</med-eq>
             </bgp-conditions>
         </conditions>

      @line 3: MED match.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4

         {
             "conditions": {
                 "bgp-conditions" : {
                     "med-eq": 100
                 }
             }
         }

      @line 4: MED match.

Match Next Hop
''''''''''''''
.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <next-hop-in>192.168.2.2</next-hop-in>
                 <next-hop-in>42.42.42.42</next-hop-in>
             </bgp-conditions>
         </conditions>

      @line 3: Next hop match.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4

         {
             "conditions": {
                 "bgp-conditions" : {
                     "next-hop-in": [
                         "192.168.2.2",
                         "42.42.42.42"
                     ]
                 }
             }
         }

      @line 4: Next hop match.

Match VPN Non member
''''''''''''''''''''

True if Route Targets attributes does not match with any Route Target Contrain advertized per Advertized peer.

.. tabs::

   .. tab:: XML

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <conditions>
             <bgp-conditions xmlns="http://openconfig.net/yang/bgp-policy">
                 <vpn-non-member xmlns="urn:opendaylight:params:xml:ns:yang:odl:bgp:default:policy"/>
             </bgp-conditions>
         </conditions>

      @line 3: VPN Non member match.

   .. tab:: JSON

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4

         {
             "conditions": {
                 "bgp-conditions" : {
                     "vpn-non-member": {
                     }
                 }
             }
         }

      @line 4: Next hop match.
