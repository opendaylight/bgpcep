
# Create network and base docker(to be used only first time)
./setUp.sh


# Create 3 containers node and configure them as clustering
./configureNodes.sh


# Remove containers
./cleanUp.sh


