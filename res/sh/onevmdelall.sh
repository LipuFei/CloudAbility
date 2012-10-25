 for i in $(onevm list |grep users | cut -f"2" -d" " ); do onevm delete $i; done
