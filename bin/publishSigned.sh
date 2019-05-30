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
sbt ++2.13.0-RC3 \
    utestJS/publishSigned \
    utestJVM/publishSigned

SCALAJS_VERSION=1.0.0-M8 sbt ++2.11.12 utestJS/publishSigned
SCALAJS_VERSION=1.0.0-M8 sbt ++2.12.8 utestJS/publishSigned
SCALAJS_VERSION=1.0.0-M8 sbt ++2.13.0-RC3 utestJS/publishSigned

SCALANATIVE_VERSION=0.4.0-M2 sbt ++2.11.12 utestNative/publishSigned
