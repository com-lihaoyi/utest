#!/usr/bin/env bash
sbt ++2.10.7 \
    utestJS/publishSigned \
    utestJVM/publishSigned
sbt ++2.11.12 \
    utestJS/publishSigned \
    utestJVM/publishSigned \
    utestNative/publishSigned
sbt ++2.12.8 \
    utestJS/publishSigned \
    utestJVM/publishSigned
sbt ++2.13.0-RC2 \
    utestJS/publishSigned \
    utestJVM/publishSigned

SCALAJS_VERSION=1.0.0-M7 sbt ++2.11.12 utestJS/publishSigned
SCALAJS_VERSION=1.0.0-M7 sbt ++2.12.8 utestJS/publishSigned
SCALAJS_VERSION=1.0.0-M7 sbt ++2.13.0-RC2 utestJS/publishSigned
