#!/usr/bin/env bash
set -eux

VERSION=$1
coursier fetch \
  com.lihaoyi:utest_2.10:$VERSION \
  com.lihaoyi:utest_2.11:$VERSION \
  com.lihaoyi:utest_2.12:$VERSION \
  com.lihaoyi:utest_2.13:$VERSION \
  com.lihaoyi:utest_2.13:$VERSION \
  com.lihaoyi:utest_native0.3_2.11:$VERSION \
  com.lihaoyi:utest_native0.4.0-M2_2.11:$VERSION \
  com.lihaoyi:utest_sjs0.6_2.10:$VERSION \
  com.lihaoyi:utest_sjs0.6_2.11:$VERSION \
  com.lihaoyi:utest_sjs0.6_2.12:$VERSION \
  com.lihaoyi:utest_sjs0.6_2.13:$VERSION \
  com.lihaoyi:utest_sjs0.6_2.13:$VERSION \
  com.lihaoyi:utest_sjs1.0.0-M8_2.11:$VERSION \
  com.lihaoyi:utest_sjs1.0.0-M8_2.12:$VERSION \
  com.lihaoyi:utest_sjs1.0.0-M8_2.13:$VERSION \
  -r sonatype:public
