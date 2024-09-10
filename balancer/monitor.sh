#!/bin/bash

cfg_dir=$1; shift

while ! ls "${cfg_dir}"/* >/dev/null 2>&1; do
  echo "Waiting for '${cfg_dir}/*' to appear"
  sleep 2
done

cat /etc/haproxy-base.cfg "${cfg_dir}"/* > /tmp/haproxy.cfg

haproxy -W -db -f /tmp/haproxy.cfg "${@}" &
svr_pid=$!

cfg_prev=`sha256sum "${cfg_dir}"/*`
while true; do
  cfg_cur=`sha256sum "${cfg_dir}"/*`
  if [ "${cfg_cur}" != "${cfg_prev}" ]; then
    echo "Sending USR2 to ${svr_pid} to reload config"
    cat /etc/haproxy-base.cfg "${cfg_dir}"/* > /tmp/haproxy.cfg
    kill -USR2 ${svr_pid}
    cfg_prev="${cfg_cur}"
  fi
  sleep 2
done

