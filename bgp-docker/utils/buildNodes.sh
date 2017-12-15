echo "## Build nodes"
for i in 1 2 3
do
   docker run --rm -d --name node$i -it bgp-docker
   ./utils/findIp.sh node$i
done
