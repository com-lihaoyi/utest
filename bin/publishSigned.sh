#!/usr/bin/env bash
sbt ++2.10.6 \
    utestJS/publishSigned \
    utestJVM/publishSigned
sbt ++2.11.12 \
    utestJS/publishSigned \
    utestJVM/publishSigned \
    utestNative/publishSigned
sbt ++2.12.4 \
    utestJS/publishSigned \
    utestJVM/publishSigned
sbt ++2.13.0-M3 \
    utestJS/publishSigned \
    utestJVM/publishSigned

SCALAJS_VERSION=1.0.0-M3 sbt ++2.11.12 utestJS/publishSigned
SCALAJS_VERSION=1.0.0-M3 sbt ++2.12.4 utestJS/publishSigned
SCALAJS_VERSION=1.0.0-M3 sbt ++2.13.0-M3 utestJS/publishSigned
