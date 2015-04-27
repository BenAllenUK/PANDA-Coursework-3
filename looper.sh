#!/usr/bin/env bash
function finish {
    kill ${serverProcess}
    kill ${judgeProcess}
    kill ${playerProcess}
}
trap finish EXIT

kill $(ps aux | grep '[n]ode' | awk '{print $2}')

node server/server_service.js &
serverProcess=$!

echo "server started"

ant judge &
judgeProcess=$!

echo "judge started"

sleep 3

echo "starting playerclient"

> output.txt

ant players > output.txt 2>&1 &
playerProcess=$!

#GAME_OVER [White, Red, Blue, Yellow, Green]

lastLine="poop"

tail -f output.txt | while read LOGLINE
do
    lastLine=${LOGLINE}
    echo "line: ${LOGLINE}"
   [[ "${LOGLINE}" == *"[java] GUI"* ]] && pkill -P $$ tail
done

echo "Finishing"

echo "last line was $lastLine"

kill ${serverProcess}
kill ${judgeProcess}
kill ${playerProcess}

exit