/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.common.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * A simple read-optimized map implementation that synchronizes only writes and does a full copy on each modification
 */

/*
* (1)这个数据结构在高并发情况下是线程安全的，因为在写的时候put()使用了synchronized关键字
* (2)正是因为put方法使用了synchronized，如果有大量的写操作，那性能就会非常低。因而这个数据结构只适合读多写少的情况；
*
* RecordAccumulator中的ConcurrentMap<TopicPartition, Deque<ProducerBatch>> batches，本质是分区和队列的映射关系就用的这一数据结构;
* batches的应用场景看好就是读多写少的场景
*
* batches:
*  读数据：每生产一条消息，都会根据分区从batches里读取分区对应的Deque<ProducerBatch>；
*  写数据：因为batches是分区和队列的映射关系，有多少分区就写多少次。
*  很明显，分区数量肯定远小于消息数量，因而是一个读多写少的场景，使用CopyOnWriteMap就十分合适。
*
* */
public class CopyOnWriteMap<K, V> implements ConcurrentMap<K, V> {
    /*
    * 核心变量是一个map,被volatile关键字修饰。在多线程情况下，如果这个map的值发生变化，其他线程也可见。
    * */
    private volatile Map<K, V> map;

    public CopyOnWriteMap() {
        this.map = Collections.emptyMap();
    }

    public CopyOnWriteMap(Map<K, V> map) {
        this.map = Collections.unmodifiableMap(map);
    }

    @Override
    public boolean containsKey(Object k) {
        return map.containsKey(k);
    }

    @Override
    public boolean containsValue(Object v) {
        return map.containsValue(v);
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public V get(Object k) {
        return map.get(k);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public synchronized void clear() {
        this.map = Collections.emptyMap();
    }

    @Override
    /*
    * 线程安全的
    * */
    public synchronized V put(K k, V v) {
        Map<K, V> copy = new HashMap<K, V>(this.map);
        V prev = copy.put(k, v);
        this.map = Collections.unmodifiableMap(copy);
        return prev;
    }

    @Override
    public synchronized void putAll(Map<? extends K, ? extends V> entries) {
        Map<K, V> copy = new HashMap<K, V>(this.map);
        copy.putAll(entries);
        this.map = Collections.unmodifiableMap(copy);
    }

    @Override
    public synchronized V remove(Object key) {
        Map<K, V> copy = new HashMap<K, V>(this.map);
        V prev = copy.remove(key);
        this.map = Collections.unmodifiableMap(copy);
        return prev;
    }

    @Override
    public synchronized V putIfAbsent(K k, V v) {
        if (!containsKey(k))
            return put(k, v);
        else
            return get(k);
    }

    @Override
    public synchronized boolean remove(Object k, Object v) {
        if (containsKey(k) && get(k).equals(v)) {
            remove(k);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized boolean replace(K k, V original, V replacement) {
        if (containsKey(k) && get(k).equals(original)) {
            put(k, replacement);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized V replace(K k, V v) {
        if (containsKey(k)) {
            return put(k, v);
        } else {
            return null;
        }
    }

}
