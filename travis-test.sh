#!/usr/bin/env bash
# Licensed under the Apache License, Version 2.0
# Adapted from https://github.com/paulp/psp-std/blob/master/bin/test

#    && gradle :agent-test:stoppable-instrumentation-spec \
runTests () {
  ./gradlew :agent:test \
    && ./gradlew :agent-test:test \
    && ./gradlew :agent-test:multi-mixins-spec \
    && ./gradlew :agent-test:simple-instrumentation-spec \
    && ./gradlew :agent-test:attach-in-runtime-spec \
    || exit 1
  echo "[info] $(date) - finished gradle test"
}

stripTerminalEscapeCodes () {
  sed -r "s/\x1B\[([0-9]{1,2}(;[0-9]{1,2})?)?[mGKM]//g"
}

mkRegex () { ( IFS="|" && echo "$*" ); }

filterOutput() {
  while read line; do
    if ! [[ $(echo $line | stripTerminalEscapeCodes) =~ $excludeRegex ]] ; then
      echo $line
    fi
  done
}

main() {
  # gradle output filter
  local excludeRegex=$(mkRegex \
    '\[info\] (Resolving|Loading|Updating|Packaging|Done updating|downloading| \[SUCCESSFUL \])' \
    're[-]run with [-]unchecked for details' \
    'one warning found'
  )

  echo "[info] $(date) - starting gradle test"
  (set -o pipefail && runTests |& filterOutput)
}

main $@
