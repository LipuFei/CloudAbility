#!/bin/bash
cd $(dirname $(readlink -f "$0"))
user=$(grep ONE.USER ../../config/cloud.config |cut -d " " -f"3")
scp -r ../../../CloudAbility $user@fs3.das4.tudelft.nl:~/
