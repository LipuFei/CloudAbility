#!/bin/bash
onevm show $1 |grep IP | cut -d"\"" -f2 |head -n1
