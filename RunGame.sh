#!/usr/bin/env bash
function finish {
    kill ${serverProcess}
    kill ${judgeProcess}
    kill ${playerProcess}
}
trap finish EXIT

node server/server_service.js &
serverProcess=$!

echo "server started"

ant judge &
judgeProcess=$!

echo "judge started"
sleep 2

ant players &
playerProcess=$!

wait ${playerProcess}

echo "Finishing"

exit