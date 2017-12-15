# Connects to local ODL running, 
#configured with 
#	<router-id>127.0.0.1</router-id>
#	<neighbor-address>127.0.0.2</neighbor-address>
#env exabgp.tcp.bind=127.0.0.2 exabgp.tcp.port=179 ./sbin/exabgp ./etc/bgpcep/default.conf -d
EXABGP_PATH="/exabgp-4.0.2"
EXABGP_CONFIGS=$EXABGP_PATH"/etc/bgpcep"

.$EXABGP_PATH/sbin/exabgp .$EXABGP_CONFIGS/defaultAddPath.conf -d
