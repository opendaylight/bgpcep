if [ -z "$1" ]
  then
    echo "No argument supplied, please provide feature ( bgp / bmp / pcep )"
    exit 1
fi
./utils/createNetwork.sh &
./utils/configureNodeAsCluster.sh $1
