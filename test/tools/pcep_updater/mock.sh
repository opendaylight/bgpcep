#!/bin/sh
# shell script which flaps pcc mock session three times with given intervals of time
# $1 pcc-mock filename
# $2 pcc-mock system ip
# $3 ODL system ip
# $4 number of pcc sessions
# $5 number of lsps for each pcc session
# $6 log file name
# $7 interval time between start/stop pcc sessions

## Permform some validation on input arguments.
if [ -z "$7" ]; then
                  echo "Missing arguments, exiting.."
                  exit 1
fi

for i in {1..3}
do
                  echo    "----------------------------------------------------------"
                  java -jar ~/$1 --local-address $2 --remote-address $3 --pcc $4 --lsp $5 &>> /tmp/throughpcep_$6.log

                  process_id=`/bin/ps -fu $USER| grep "$HOME/$1" | grep -v "grep" | awk '{print $2}'`
                  echo "PCC MOCK Process id for $a iteration is $process_id"
                  sleep $7m

                  ps -ef |grep "$HOME/$1"    | grep -v "grep"| awk '{print $2}' | xargs kill

done
