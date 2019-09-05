#!/bin/bash
pushd ${SEMAPHORE_PROJECT_DIR}
rm mbta_otp.zip
zip mbta_otp.zip mbta/Procfile otp-1.4.0*-shaded.jar var/graphs/mbta/Graph.obj var/graphs/mbta/*.json
popd
