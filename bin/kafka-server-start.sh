#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# $#,传递给脚本或者函数的参数个数；$0，当前进程名称；
if [ $# -lt 1 ];
then
	echo "USAGE: $0 [-daemon] server.properties [--override property=value]*"
	exit 1
fi
#dirname命令可以返回文件所在的目录。$0 表示当前动行的命令名;此写法的作用为： 切换到 脚本 所在的目录;通过debug，base_dir的值为 .
base_dir=$(dirname $0)

if [ "x$KAFKA_LOG4J_OPTS" = "x" ]; then
    export KAFKA_LOG4J_OPTS="-Dlog4j.configuration=file:$base_dir/../config/log4j.properties"
fi
# 配置KAFKA_LOG4J_OPTS参数。这种前面加“x”的用法是shell的一种技巧，判断KAFKA_LOG4J_OPTS参数是否为空。

if [ "x$KAFKA_HEAP_OPTS" = "x" ]; then
    export KAFKA_HEAP_OPTS="-Xmx1G -Xms1G"
fi

EXTRA_ARGS=${EXTRA_ARGS-'-name kafkaServer -loggc'}

COMMAND=$1
case $COMMAND in
  -daemon)
    EXTRA_ARGS="-daemon "$EXTRA_ARGS
    shift
    ;;
  *)
    ;;
esac
# 判断第一个参数是不是“daemon”，如果是的话，编入EXTRA_ARGS变量，下面要送给新的命令，然后使用shift从入参栈中移除掉这个参数，后面会使用“$@”来使用剩余的入参
exec $base_dir/kafka-run-class.sh $EXTRA_ARGS kafka.Kafka "$@"
# 启动kafka-run-class.sh脚本，把上述参数送进去
