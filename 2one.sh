#!/bin/bash
cd $(dirname $(readlink -f "$0"))
user=$(grep ONE.USER ../../config/cloud.config |cut -d " " -f"3")
rsync --exclude=.git/* --progress -r ../../../CloudAbility $user@fs3.das4.tudelft.nl:~/
