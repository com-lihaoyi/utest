#!/usr/bin/env bash
set -eux

VERSION=$1
coursier fetch \
  com.lihaoyi:utest_2.10:$VERSION \
  com.lihaoyi:utest_2.11:$VERSION \
  com.lihaoyi:utest_2.12:$VERSION \
  com.lihaoyi:utest_2.13.0-M5:$VERSION \
  com.lihaoyi:utest_2.13.0-M5:$VERSION \
  com.lihaoyi:utest_native0.3_2.11:$VERSION \
  com.lihaoyi:utest_sjs0.6_2.10:$VERSION \
  com.lihaoyi:utest_sjs0.6_2.11:$VERSION \
  com.lihaoyi:utest_sjs0.6_2.12:$VERSION \
  com.lihaoyi:utest_sjs0.6_2.13.0-M5:$VERSION \
  com.lihaoyi:utest_sjs0.6_2.13.0-M5:$VERSION \
  com.lihaoyi:utest_sjs1.0.0-M5_2.11:$VERSION \
  com.lihaoyi:utest_sjs1.0.0-M5_2.12:$VERSION \
  -r sonatype:releases
  # com.lihaoyi:utest_sjs1.0.0-M5_2.13.0-M5:$VERSION \ # Did not release
