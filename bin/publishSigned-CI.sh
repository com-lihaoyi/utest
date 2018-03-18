#!/usr/bin/env bash
if [[ "$TRAVIS_SECURE_ENV_VARS" == true && "$CI_PUBLISH" == true && -n "$TRAVIS_TAG" ]]; then
  echo "Publishing to sonatype..."
  git log | head -n 20
  echo "$PGP_SECRET" | base64 --decode | gpg --import
  ./bin/publishSigned.sh
else
  echo "Skipping publish, branch=$TRAVIS_BRANCH"
fi
