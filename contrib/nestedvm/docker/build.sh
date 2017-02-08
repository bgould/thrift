#/bin/bash

docker build -t thrift-nestedvm docker
docker run --rm -u $(id -u):$(id -g) \
  -v `pwd`/../..:/usr/local/src/thrift thrift-nestedvm \
  bash -c "cd contrib/nestedvm && make clean && make"
