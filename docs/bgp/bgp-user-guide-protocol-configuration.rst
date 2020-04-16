.. _bgp-user-guide-protocol-configuration:

Protocol Configuration
======================
As a first step, a new protocol instance needs to be configured.
It is a very basic configuration conforming with RFC4271.

.. note:: RIB policy must already be configured and present before configuring the protocol.

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols``

**Method:** ``POST``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json
   :linenos:
   :emphasize-lines: 5,13,14

	{
	    "protocol": {
		     "_xmlns": "http://openconfig.net/yang/network-instance",
		     "name": "bgp-example",
		     "identifier": {
		    		     "_text": "x:BGP",
		    		     "_xmlns:x": "http://openconfig.net/yang/policy-types"
				  },
		     "bgp": {
		   	 "_xmlns": "urn:opendaylight:params:xml:ns:yang:bgp:openconfig-extensions",
		    	 "global": {
		    	   	"config": {
			               "router-id": "192.0.2.2",
			               "as": "65000"  
			        	  },
		           	"apply-policy": {
		            	    	     "config": {
					                "default-export-policy": "REJECT-ROUTE",
					                "default-import-policy": "REJECT-ROUTE",
					                "export-policy": "default-odl-export-policy",
					                "import-policy": "default-odl-import-policy"
					              }
		        		   }
		    	 	  }
			 }
	    	}
	}


@line 5: The unique protocol instance identifier.

@line 13: BGP Identifier of the speaker.

@line 14: Local autonomous system number of the speaker. Note that, OpenDaylight BGP implementation supports four-octet AS numbers only.

@line 20: Default ODL Import Policy.

@line 21: Default ODL Export Policy.

-----

The new instance presence can be verified via REST:

**URL:** ``/restconf/operational/bgp-rib:bgp-rib/rib/bgp-example``

**Method:** ``GET``

**Response Body:**

.. code-block:: json
   :linenos:
   :emphasize-lines: 5,6

	{
	    "rib": {
		"_xmlns": "urn:opendaylight:params:xml:ns:yang:bgp-rib",
		"id": "bgp-example",
		"loc-rib": {
		   	"tables": {
		               "afi": {
		         	    	"_text": "x:ipv4-address-family",
		         	   	"_xmlns:x": "urn:opendaylight:params:xml:ns:yang:bgp-types"
		        	    },
			       "attributes": {
			         	       "uptodate": "true"
			      	    },
			       "ipv4-routes": {
				               "_xmlns": "urn:opendaylight:params:xml:ns:yang:bgp-inet"
				    },
			       "safi": {
				    	"_text": "x:unicast-subsequent-address-family",
				    	"_xmlns:x": "urn:opendaylight:params:xml:ns:yang:bgp-types"
				    }
		   		}
			 }
	         }
	}



@line 5: Loc-RIB - Per-protocol instance RIB, which contains the routes that have been selected by local BGP speaker's decision process.

@line 6: The BGP-4 supports carrying IPv4 prefixes, such routes are stored in *ipv4-address-family*/*unicast-subsequent-address-family* table.
