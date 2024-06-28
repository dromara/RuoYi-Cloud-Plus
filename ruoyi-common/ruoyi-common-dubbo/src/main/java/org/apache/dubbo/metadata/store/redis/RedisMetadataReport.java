/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.metadata.store.redis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MappingChangedEvent;
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.ServiceNameMapping;
import org.apache.dubbo.metadata.report.identifier.*;
import org.apache.dubbo.metadata.report.support.AbstractMetadataReport;
import org.apache.dubbo.rpc.RpcException;
import redis.clients.jedis.*;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.util.JedisClusterCRC16;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_RESPONSE;
import static org.apache.dubbo.metadata.MetadataConstants.META_DATA_STORE_TAG;
import static org.apache.dubbo.metadata.ServiceNameMapping.DEFAULT_MAPPING_GROUP;
import static org.apache.dubbo.metadata.ServiceNameMapping.getAppNames;
import static org.apache.dubbo.metadata.report.support.Constants.DEFAULT_METADATA_REPORT_CYCLE_REPORT;

/**
 * RedisMetadataReport 是基于 Redis 的元数据报告实现类
 */
public class RedisMetadataReport extends AbstractMetadataReport {
    private static final String REDIS_DATABASE_KEY = "database";
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RedisMetadataReport.class);
    // 受保护的 JedisPool 实例，用于测试
    protected JedisPool pool;
    // Redis 集群节点集合
    private Set<HostAndPort> jedisClusterNodes;
    private int timeout;
    private String password;
    private final String root;
    // 映射数据监听器映射表
    private final ConcurrentHashMap<String, MappingDataListener> mappingDataListenerMap = new ConcurrentHashMap<>();
    private SetParams jedisParams = SetParams.setParams();

    /**
     * 构造方法，根据给定的 URL 初始化 RedisMetadataReport
     *
     * @param url 元数据中心的 URL
     */
    public RedisMetadataReport(URL url) {
        super(url);
        timeout = url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT);
        password = url.getPassword();
        this.root = url.getGroup(DEFAULT_ROOT);

        // 设置默认的周期性报告时间
        if (url.getParameter(CYCLE_REPORT_KEY, DEFAULT_METADATA_REPORT_CYCLE_REPORT)) {
            // TTL 默认是周期报告时间的两倍
            jedisParams.ex(ONE_DAY_IN_MILLISECONDS * 2);
        }

        // 判断是否为集群模式
        if (url.getParameter(CLUSTER_KEY, false)) {
            jedisClusterNodes = new HashSet<>();
            List<URL> urls = url.getBackupUrls();
            for (URL tmpUrl : urls) {
                jedisClusterNodes.add(new HostAndPort(tmpUrl.getHost(), tmpUrl.getPort()));
            }
        } else {
            // 单机模式下的 Redis 数据库编号，默认为 0
            int database = url.getParameter(REDIS_DATABASE_KEY, 0);
            pool = new JedisPool(new JedisPoolConfig(), url.getHost(), url.getPort(), timeout, password, database);
        }
    }

    /**
     * 存储提供者元数据的具体实现
     *
     * @param providerMetadataIdentifier 提供者元数据标识符
     * @param serviceDefinitions         服务定义信息
     */
    @Override
    protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
        this.storeMetadata(providerMetadataIdentifier, serviceDefinitions);
    }

    /**
     * 存储消费者元数据的具体实现
     *
     * @param consumerMetadataIdentifier 消费者元数据标识符
     * @param value                      元数据值
     */
    @Override
    protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String value) {
        this.storeMetadata(consumerMetadataIdentifier, value);
    }

    /**
     * 存储服务元数据的具体实现
     *
     * @param serviceMetadataIdentifier 服务元数据标识符
     * @param url                       服务URL编码后的完整字符串
     */
    @Override
    protected void doSaveMetadata(ServiceMetadataIdentifier serviceMetadataIdentifier, URL url) {
        this.storeMetadata(serviceMetadataIdentifier, URL.encode(url.toFullString()));
    }

    /**
     * 移除元数据的具体实现
     *
     * @param serviceMetadataIdentifier 服务元数据标识符
     */
    @Override
    protected void doRemoveMetadata(ServiceMetadataIdentifier serviceMetadataIdentifier) {
        this.deleteMetadata(serviceMetadataIdentifier);
    }

    /**
     * 获取导出的URL列表
     *
     * @param metadataIdentifier 服务元数据标识符
     * @return 导出的URL列表，如果内容为空则返回空列表
     */
    @Override
    protected List<String> doGetExportedURLs(ServiceMetadataIdentifier metadataIdentifier) {
        String content = getMetadata(metadataIdentifier);
        if (StringUtils.isEmpty(content)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(Arrays.asList(URL.decode(content)));
    }

    /**
     * 存储订阅者数据的具体实现
     *
     * @param subscriberMetadataIdentifier 订阅者元数据标识符
     * @param urlListStr                   URL列表字符串
     */
    @Override
    protected void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, String urlListStr) {
        this.storeMetadata(subscriberMetadataIdentifier, urlListStr);
    }

    /**
     * 获取订阅的URL列表
     *
     * @param subscriberMetadataIdentifier 订阅者元数据标识符
     * @return 订阅的URL列表
     */
    @Override
    protected String doGetSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier) {
        return this.getMetadata(subscriberMetadataIdentifier);
    }

    /**
     * 获取服务定义
     *
     * @param metadataIdentifier 元数据标识符
     * @return 服务定义内容
     */
    @Override
    public String getServiceDefinition(MetadataIdentifier metadataIdentifier) {
        return this.getMetadata(metadataIdentifier);
    }

    /**
     * 存储元数据的通用方法，根据是否有连接池选择存储方式
     *
     * @param metadataIdentifier 元数据标识符
     * @param v                  元数据值
     */
    private void storeMetadata(BaseMetadataIdentifier metadataIdentifier, String v) {
        if (pool != null) {
            storeMetadataStandalone(metadataIdentifier, v);
        } else {
            storeMetadataInCluster(metadataIdentifier, v);
        }
    }

    /**
     * 在集群模式下存储元数据
     *
     * @param metadataIdentifier 元数据标识符
     * @param v                  元数据值
     */
    private void storeMetadataInCluster(BaseMetadataIdentifier metadataIdentifier, String v) {
        try (JedisCluster jedisCluster =
                 new JedisCluster(jedisClusterNodes, timeout, timeout, 2, password, new GenericObjectPoolConfig<>())) {
            jedisCluster.set(metadataIdentifier.getIdentifierKey() + META_DATA_STORE_TAG, v, jedisParams);
        } catch (Throwable e) {
            String msg =
                "Failed to put " + metadataIdentifier + " to redis cluster " + v + ", cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    /**
     * 在单机模式下存储元数据
     *
     * @param metadataIdentifier 元数据标识符
     * @param v                  元数据值
     */
    private void storeMetadataStandalone(BaseMetadataIdentifier metadataIdentifier, String v) {
        try (Jedis jedis = pool.getResource()) {
            jedis.set(metadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), v, jedisParams);
        } catch (Throwable e) {
            String msg = "Failed to put " + metadataIdentifier + " to redis " + v + ", cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    /**
     * 删除元数据
     *
     * @param metadataIdentifier 元数据标识符
     */
    private void deleteMetadata(BaseMetadataIdentifier metadataIdentifier) {
        if (pool != null) {
            deleteMetadataStandalone(metadataIdentifier);
        } else {
            deleteMetadataInCluster(metadataIdentifier);
        }
    }

    /**
     * 在集群模式下删除元数据
     *
     * @param metadataIdentifier 元数据标识符
     */
    private void deleteMetadataInCluster(BaseMetadataIdentifier metadataIdentifier) {
        try (JedisCluster jedisCluster =
                 new JedisCluster(jedisClusterNodes, timeout, timeout, 2, password, new GenericObjectPoolConfig<>())) {
            jedisCluster.del(metadataIdentifier.getIdentifierKey() + META_DATA_STORE_TAG);
        } catch (Throwable e) {
            String msg = "Failed to delete " + metadataIdentifier + " from redis cluster , cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    /**
     * 在单机模式下删除元数据
     *
     * @param metadataIdentifier 元数据标识符
     */
    private void deleteMetadataStandalone(BaseMetadataIdentifier metadataIdentifier) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(metadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
        } catch (Throwable e) {
            String msg = "Failed to delete " + metadataIdentifier + " from redis , cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    /**
     * 获取元数据
     *
     * @param metadataIdentifier 元数据标识符
     * @return 元数据值
     */
    private String getMetadata(BaseMetadataIdentifier metadataIdentifier) {
        if (pool != null) {
            return getMetadataStandalone(metadataIdentifier);
        } else {
            return getMetadataInCluster(metadataIdentifier);
        }
    }

    /**
     * 在集群模式下获取元数据
     *
     * @param metadataIdentifier 元数据标识符
     * @return 元数据值
     */
    private String getMetadataInCluster(BaseMetadataIdentifier metadataIdentifier) {
        try (JedisCluster jedisCluster =
                 new JedisCluster(jedisClusterNodes, timeout, timeout, 2, password, new GenericObjectPoolConfig<>())) {
            return jedisCluster.get(metadataIdentifier.getIdentifierKey() + META_DATA_STORE_TAG);
        } catch (Throwable e) {
            String msg = "Failed to get " + metadataIdentifier + " from redis cluster , cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    /**
     * 在单机模式下获取元数据
     *
     * @param metadataIdentifier 元数据标识符
     * @return 元数据值
     */
    private String getMetadataStandalone(BaseMetadataIdentifier metadataIdentifier) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get(metadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
        } catch (Throwable e) {
            String msg = "Failed to get " + metadataIdentifier + " from redis , cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    /**
     * 使用Redis哈希存储类和应用程序名称的映射关系
     * <p>
     * 键：默认为 'dubbo:mapping'
     * 字段：类（serviceInterface）
     * 值：应用程序名称列表
     *
     * @param serviceInterface    类名（作为字段）
     * @param defaultMappingGroup 默认映射组 {@link ServiceNameMapping#DEFAULT_MAPPING_GROUP}
     * @param newConfigContent    新的应用程序名称列表
     * @param ticket              先前的应用程序名称列表
     * @return 是否成功注册映射关系
     */
    @Override
    public boolean registerServiceAppMapping(
        String serviceInterface, String defaultMappingGroup, String newConfigContent, Object ticket) {
        try {
            if (null != ticket && !(ticket instanceof String)) {
                throw new IllegalArgumentException("redis publishConfigCas requires stat type ticket");
            }
            String pathKey = buildMappingKey(defaultMappingGroup);

            return storeMapping(pathKey, serviceInterface, newConfigContent, (String) ticket);
        } catch (Exception e) {
            logger.warn(TRANSPORT_FAILED_RESPONSE, "", "", "redis publishConfigCas failed.", e);
            return false;
        }
    }

    /**
     * 根据是否存在 Redis 连接池选择存储映射关系的方法。
     * 如果存在 Redis 连接池，则使用单机存储方式 {@link #storeMappingStandalone(String, String, String, String)}；
     * 否则，使用集群存储方式 {@link #storeMappingInCluster(String, String, String, String)}。
     *
     * @param key    存储键
     * @param field  存储字段
     * @param value  存储值
     * @param ticket 事务票据，用于 CAS 操作
     * @return 存储是否成功
     */
    private boolean storeMapping(String key, String field, String value, String ticket) {
        if (pool != null) {
            return storeMappingStandalone(key, field, value, ticket);
        } else {
            return storeMappingInCluster(key, field, value, ticket);
        }
    }

    /**
     * 在 Redis 集群中存储映射关系
     * 使用 Redis 集群的方式存储给定键和字段的映射关系，并实现乐观锁 CAS 操作
     * 如果旧值为空或与给定的事务票据匹配，则更新字段的值为新值，并发布更新事件
     * 使用 WATCH 和 MULTI 来实现事务操作
     *
     * @param key    存储键
     * @param field  存储字段
     * @param value  存储值
     * @param ticket 事务票据，用于 CAS 操作
     * @return 存储是否成功
     * @throws RpcException 存储过程中发生的异常，以及失败的原因
     */
    private boolean storeMappingInCluster(String key, String field, String value, String ticket) {
        try (JedisCluster jedisCluster =
                 new JedisCluster(jedisClusterNodes, timeout, timeout, 2, password, new GenericObjectPoolConfig<>())) {
            Jedis jedis = new Jedis(jedisCluster.getConnectionFromSlot(JedisClusterCRC16.getSlot(key)));
            jedis.watch(key);
            String oldValue = jedis.hget(key, field);
            if (null == oldValue || null == ticket || oldValue.equals(ticket)) {
                Transaction transaction = jedis.multi();
                transaction.hset(key, field, value);
                List<Object> result = transaction.exec();
                if (null != result) {
                    jedisCluster.publish(buildPubSubKey(), field);
                    return true;
                }
            } else {
                jedis.unwatch();
            }
            jedis.close();
        } catch (Throwable e) {
            String msg = "Failed to put " + key + ":" + field + " to redis " + value + ", cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
        return false;
    }

    /**
     * 在单机Redis中存储映射关系
     * 使用 'watch' 实现CAS（比较并交换）
     *
     * @param key    Redis键
     * @param field  Redis哈希字段（类名）
     * @param value  新的应用程序名称列表
     * @param ticket 先前的应用程序名称列表
     * @return 是否成功存储映射关系
     */
    private boolean storeMappingStandalone(String key, String field, String value, String ticket) {
        try (Jedis jedis = pool.getResource()) {
            jedis.watch(key);
            String oldValue = jedis.hget(key, field);
            if (null == oldValue || null == ticket || oldValue.equals(ticket)) {
                Transaction transaction = jedis.multi();
                transaction.hset(key, field, value);
                List<Object> result = transaction.exec();
                if (null != result) {
                    jedis.publish(buildPubSubKey(), field);
                    return true;
                }
            }
            jedis.unwatch();
        } catch (Throwable e) {
            String msg = "Failed to put " + key + ":" + field + " to redis " + value + ", cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
        return false;
    }

    /**
     * 构建映射关键字，用于存储服务类和应用名称的 Redis 哈希表键
     * 结合根路径和默认映射组名构建完整的映射键
     *
     * @param defaultMappingGroup 默认映射组名 {@link ServiceNameMapping#DEFAULT_MAPPING_GROUP}
     * @return 构建的映射关键字
     */
    private String buildMappingKey(String defaultMappingGroup) {
        return this.root + GROUP_CHAR_SEPARATOR + defaultMappingGroup;
    }

    /**
     * 构建发布订阅键，用于 Redis 发布-订阅模式中的通道名称
     * 结合默认映射组名和队列键构建完整的发布订阅键
     *
     * @return 构建的发布订阅键
     */
    private String buildPubSubKey() {
        return buildMappingKey(DEFAULT_MAPPING_GROUP) + GROUP_CHAR_SEPARATOR + QUEUES_KEY;
    }

    /**
     * 根据服务键和分组获取配置项
     * 使用分组构建映射键，并获取映射数据，然后返回一个配置项对象
     *
     * @param serviceKey 服务键，用于标识特定的服务
     * @param group      分组，用于构建映射键
     * @return 配置项对象，包含从映射数据中获取的内容
     */
    @Override
    public ConfigItem getConfigItem(String serviceKey, String group) {
        String key = buildMappingKey(group);
        String content = getMappingData(key, serviceKey);
        return new ConfigItem(content, content);
    }

    /**
     * 根据键和字段从 Redis 中获取映射数据
     * 如果连接池不为空，则使用独立模式获取数据；否则使用集群模式
     *
     * @param key   键，用于定位数据的存储位置
     * @param field 字段，用于定位具体的数据项
     * @return 获取到的映射数据
     */
    private String getMappingData(String key, String field) {
        if (pool != null) {
            return getMappingDataStandalone(key, field);
        } else {
            return getMappingDataInCluster(key, field);
        }
    }

    /**
     * 从 Redis 集群中获取指定键和字段的映射数据
     *
     * @param key   Redis 哈希表的键
     * @param field 哈希表中的字段
     * @return 返回键和字段对应的值，如果获取失败则抛出异常
     * @throws RpcException 如果从 Redis 集群获取数据失败，抛出该异常
     */
    private String getMappingDataInCluster(String key, String field) {
        try (JedisCluster jedisCluster =
                 new JedisCluster(jedisClusterNodes, timeout, timeout, 2, password, new GenericObjectPoolConfig<>())) {
            return jedisCluster.hget(key, field);
        } catch (Throwable e) {
            String msg = "Failed to get " + key + ":" + field + " from redis cluster , cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    /**
     * 使用集群模式从 Redis 中获取映射数据
     *
     * @param key   键，用于定位数据的存储位置
     * @param field 字段，用于定位具体的数据项
     * @return 获取到的映射数据，如果获取失败则抛出 RpcException 异常
     */
    private String getMappingDataStandalone(String key, String field) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hget(key, field);
        } catch (Throwable e) {
            String msg = "Failed to get " + key + ":" + field + " from redis , cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    /**
     * 移除服务应用映射的监听器
     *
     * @param serviceKey 服务键，用于标识特定的服务
     * @param listener   映射监听器，用于处理映射变更事件
     */
    @Override
    public void removeServiceAppMappingListener(String serviceKey, MappingListener listener) {
        MappingDataListener mappingDataListener = mappingDataListenerMap.get(buildPubSubKey());
        if (null != mappingDataListener) {
            NotifySub notifySub = mappingDataListener.getNotifySub();
            notifySub.removeListener(serviceKey, listener);
            if (notifySub.isEmpty()) {
                mappingDataListener.shutdown();
            }
        }
    }

    /**
     * 启动一个线程并订阅 {@link this#buildPubSubKey()}
     * 如果 'application_names' 消息发生变化，则通知 {@link MappingListener}。
     *
     * @param serviceKey 服务键
     * @param listener   映射监听器
     * @param url        URL
     * @return 返回服务与应用映射关系的集合
     */
    @Override
    public Set<String> getServiceAppMapping(String serviceKey, MappingListener listener, URL url) {
        MappingDataListener mappingDataListener =
            ConcurrentHashMapUtils.computeIfAbsent(mappingDataListenerMap, buildPubSubKey(), k -> {
                MappingDataListener dataListener = new MappingDataListener(buildPubSubKey());
                dataListener.start();
                return dataListener;
            });
        mappingDataListener.getNotifySub().addListener(serviceKey, listener);
        return this.getServiceAppMapping(serviceKey, url);
    }

    /**
     * 获取指定服务键的服务与应用映射关系集合
     *
     * @param serviceKey 服务键
     * @param url        URL
     * @return 返回服务与应用映射关系的集合
     */
    @Override
    public Set<String> getServiceAppMapping(String serviceKey, URL url) {
        String key = buildMappingKey(DEFAULT_MAPPING_GROUP);
        return getAppNames(getMappingData(key, serviceKey));
    }

    /**
     * 获取订阅者元数据信息
     *
     * @param identifier       订阅者元数据标识符
     * @param instanceMetadata 实例元数据映射
     * @return 返回订阅者的元数据信息对象
     */
    @Override
    public MetadataInfo getAppMetadata(SubscriberMetadataIdentifier identifier, Map<String, String> instanceMetadata) {
        String content = this.getMetadata(identifier);
        return JsonUtils.toJavaObject(content, MetadataInfo.class);
    }

    /**
     * 发布应用元数据信息
     *
     * @param identifier   订阅者元数据标识符
     * @param metadataInfo 元数据信息对象
     */
    @Override
    public void publishAppMetadata(SubscriberMetadataIdentifier identifier, MetadataInfo metadataInfo) {
        this.storeMetadata(identifier, metadataInfo.getContent());
    }

    /**
     * 取消发布应用元数据信息
     *
     * @param identifier   订阅者元数据标识符
     * @param metadataInfo 元数据信息对象
     */
    @Override
    public void unPublishAppMetadata(SubscriberMetadataIdentifier identifier, MetadataInfo metadataInfo) {
        this.deleteMetadata(identifier);
    }

    // 用于测试
    public MappingDataListener getMappingDataListener() {
        return mappingDataListenerMap.get(buildPubSubKey());
    }

    /**
     * 监听 'application_names' 消息的变化并通知监听器
     */
    class NotifySub extends JedisPubSub {
        private final Map<String, Set<MappingListener>> listeners = new ConcurrentHashMap<>();

        /**
         * 添加监听器
         *
         * @param key      监听的键
         * @param listener 监听器对象
         */
        public void addListener(String key, MappingListener listener) {
            Set<MappingListener> listenerSet = listeners.computeIfAbsent(key, k -> new ConcurrentHashSet<>());
            listenerSet.add(listener);
        }

        /**
         * 移除监听器
         *
         * @param serviceKey 服务键
         * @param listener   监听器对象
         */
        public void removeListener(String serviceKey, MappingListener listener) {
            Set<MappingListener> listenerSet = this.listeners.get(serviceKey);
            if (listenerSet != null) {
                listenerSet.remove(listener);
                if (listenerSet.isEmpty()) {
                    this.listeners.remove(serviceKey);
                }
            }
        }

        /**
         * 检查监听器集合是否为空
         *
         * @return 如果监听器集合为空则返回 true，否则返回 false
         */
        public Boolean isEmpty() {
            return this.listeners.isEmpty();
        }

        /**
         * 当接收到消息时触发的方法
         *
         * @param key 消息的键
         * @param msg 接收到的消息内容
         */
        @Override
        public void onMessage(String key, String msg) {
            logger.info("sub from redis:" + key + " message:" + msg);
            String applicationNames = getMappingData(buildMappingKey(DEFAULT_MAPPING_GROUP), msg);
            MappingChangedEvent mappingChangedEvent = new MappingChangedEvent(msg, getAppNames(applicationNames));
            if (!listeners.get(msg).isEmpty()) {
                for (MappingListener mappingListener : listeners.get(msg)) {
                    mappingListener.onEvent(mappingChangedEvent);
                }
            }
        }

        /**
         * 当接收到模式消息时触发的方法
         *
         * @param pattern 模式
         * @param key     消息的键
         * @param msg     接收到的消息内容
         */
        @Override
        public void onPMessage(String pattern, String key, String msg) {
            onMessage(key, msg);
        }

        /**
         * 当成功订阅模式时触发的方法
         *
         * @param pattern            订阅的模式
         * @param subscribedChannels 订阅的频道数量
         */
        @Override
        public void onPSubscribe(String pattern, int subscribedChannels) {
            super.onPSubscribe(pattern, subscribedChannels);
        }
    }

    /**
     * 监听应用名称变化消息的线程类
     */
    class MappingDataListener extends Thread {

        private String path;

        private final NotifySub notifySub = new NotifySub();
        // for test
        protected volatile boolean running = true;

        /**
         * 构造方法，指定监听的路径
         *
         * @param path 监听的路径
         */
        public MappingDataListener(String path) {
            this.path = path;
        }

        /**
         * 获取通知订阅器
         *
         * @return 通知订阅器
         */
        public NotifySub getNotifySub() {
            return notifySub;
        }

        /**
         * 线程运行方法，持续订阅指定路径的消息
         */
        @Override
        public void run() {
            while (running) {
                if (pool != null) {
                    try (Jedis jedis = pool.getResource()) {
                        jedis.subscribe(notifySub, path);
                    } catch (Throwable e) {
                        String msg = "Failed to subscribe " + path + ", cause: " + e.getMessage();
                        logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
                        throw new RpcException(msg, e);
                    }
                } else {
                    try (JedisCluster jedisCluster = new JedisCluster(
                        jedisClusterNodes, timeout, timeout, 2, password, new GenericObjectPoolConfig<>())) {
                        jedisCluster.subscribe(notifySub, path);
                    } catch (Throwable e) {
                        String msg = "Failed to subscribe " + path + ", cause: " + e.getMessage();
                        logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
                        throw new RpcException(msg, e);
                    }
                }
            }
        }

        /**
         * 关闭方法，用于停止线程运行并取消订阅指定路径的消息
         */
        public void shutdown() {
            try {
                running = false;
                notifySub.unsubscribe(path);
            } catch (Throwable e) {
                String msg = "Failed to unsubscribe " + path + ", cause: " + e.getMessage();
                logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            }
        }
    }
}
