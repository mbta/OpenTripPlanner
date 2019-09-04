#!/bin/bash
# should be run as ./mbta/semaphore/build.sh
set -e

mbta/semaphore/update_pbf.sh
mbta/semaphore/update_gtfs.sh
mbta/semaphore/build.sh
mbta/semaphore/make_deploy.sh
