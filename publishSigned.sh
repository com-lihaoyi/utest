#!/usr/bin/env bash
sbt ++2.10.6 \
    utestJS/publishSigned \
    utestJVM/publishSigned
sbt ++2.11.11 \
    utestJS/publishSigned \
    utestJVM/publishSigned \
    utestNative/publishSigned
sbt ++2.12.3 \
    utestJS/publishSigned \
    utestJVM/publishSigned
SCALAJS_VERSION=1.0.0-M1 sbt ++2.10.6 utestJS/publishSigned
SCALAJS_VERSION=1.0.0-M1 sbt ++2.11.11 utestJS/publishSigned
SCALAJS_VERSION=1.0.0-M1 sbt ++2.12.3 utestJS/publishSigned
