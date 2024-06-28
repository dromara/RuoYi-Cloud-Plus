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
package org.apache.dubbo.metadata.report.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.metrics.metadata.event.MetadataEvent;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROXY_FAILED_EXPORT_SERVICE;
import static org.apache.dubbo.common.utils.StringUtils.replace;
import static org.apache.dubbo.metadata.report.support.Constants.*;

/**
 * 抽象的元数据上报实现类，实现了元数据上报的基本操作
 */
public abstract class AbstractMetadataReport implements MetadataReport {

    protected static final String DEFAULT_ROOT = "dubbo";

    protected static final int ONE_DAY_IN_MILLISECONDS = 60 * 24 * 60 * 1000;
    private static final int FOUR_HOURS_IN_MILLISECONDS = 60 * 4 * 60 * 1000;
    // Log output
    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    // 本地磁盘缓存，特定键值 registries 记录元数据中心列表，其他是通知的服务提供者列表
    final Properties properties = new Properties();
    private final ExecutorService reportCacheExecutor =
        Executors.newFixedThreadPool(1, new NamedThreadFactory("DubboSaveMetadataReport", true));
    final Map<MetadataIdentifier, Object> allMetadataReports = new ConcurrentHashMap<>(4);

    private final AtomicLong lastCacheChanged = new AtomicLong();
    final Map<MetadataIdentifier, Object> failedReports = new ConcurrentHashMap<>(4);
    private URL reportURL;
    boolean syncReport;
    // 本地磁盘缓存文件
    File file;
    private AtomicBoolean initialized = new AtomicBoolean(false);
    public MetadataReportRetry metadataReportRetry;
    private ScheduledExecutorService reportTimerScheduler;

    private final boolean reportMetadata;
    private final boolean reportDefinition;
    protected ApplicationModel applicationModel;

    /**
     * 构造方法，初始化元数据上报实现类
     *
     * @param reportServerURL 元数据上报服务器的URL
     */
    public AbstractMetadataReport(URL reportServerURL) {
        setUrl(reportServerURL);
        applicationModel = reportServerURL.getOrDefaultApplicationModel();

        boolean localCacheEnabled = reportServerURL.getParameter(REGISTRY_LOCAL_FILE_CACHE_ENABLED, true);
        // 启动文件保存定时器
        String defaultFilename = System.getProperty("user.home") + DUBBO_METADATA
            + reportServerURL.getApplication() + "-" + replace(reportServerURL.getAddress(), ":", "-") + CACHE;
        String filename = reportServerURL.getParameter(FILE_KEY, defaultFilename);
        File file = null;
        if (localCacheEnabled && ConfigUtils.isNotEmpty(filename)) {
            file = new File(filename);
            if (!file.exists() && file.getParentFile() != null && !file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    throw new IllegalArgumentException("Invalid service store file " + file
                        + ", cause: Failed to create directory " + file.getParentFile() + "!");
                }
            }
            // 如果文件存在，首先删除它
            if (!initialized.getAndSet(true) && file.exists()) {
                file.delete();
            }
        }
        this.file = file;
        loadProperties();
        syncReport = reportServerURL.getParameter(SYNC_REPORT_KEY, false);
        metadataReportRetry = new MetadataReportRetry(
            reportServerURL.getParameter(RETRY_TIMES_KEY, DEFAULT_METADATA_REPORT_RETRY_TIMES),
            reportServerURL.getParameter(RETRY_PERIOD_KEY, DEFAULT_METADATA_REPORT_RETRY_PERIOD));

        // 循环上报数据开关
        if (reportServerURL.getParameter(CYCLE_REPORT_KEY, DEFAULT_METADATA_REPORT_CYCLE_REPORT)) {
            reportTimerScheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DubboMetadataReportTimer", true));
            reportTimerScheduler.scheduleAtFixedRate(this::publishAll, calculateStartTime(), ONE_DAY_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        }

        this.reportMetadata = reportServerURL.getParameter(REPORT_METADATA_KEY, false);
        this.reportDefinition = reportServerURL.getParameter(REPORT_DEFINITION_KEY, true);
    }

    /**
     * 获取元数据上报服务器的URL
     *
     * @return 元数据上报服务器的URL
     */
    public URL getUrl() {
        return reportURL;
    }

    /**
     * 设置元数据上报服务器的URL
     *
     * @param url 元数据上报服务器的URL
     */
    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("metadataReport url == null");
        }
        this.reportURL = url;
    }

    /**
     * 执行保存属性操作，将属性持久化到本地磁盘缓存文件中
     *
     * @param version 缓存版本号
     */
    private void doSaveProperties(long version) {
        if (version < lastCacheChanged.get()) {
            return;
        }
        if (file == null) {
            return;
        }
        // 保存操作
        try {
            File lockfile = new File(file.getAbsolutePath() + ".lock");
            if (!lockfile.exists()) {
                lockfile.createNewFile();
            }
            try (RandomAccessFile raf = new RandomAccessFile(lockfile, "rw");
                 FileChannel channel = raf.getChannel()) {
                FileLock lock = channel.tryLock();
                if (lock == null) {
                    throw new IOException(
                        "Can not lock the metadataReport cache file " + file.getAbsolutePath()
                            + ", ignore and retry later, maybe multi java process use the file, please config: dubbo.metadata.file=xxx.properties");
                }
                // 保存
                try {
                    if (!file.exists()) {
                        file.createNewFile();
                    }

                    Properties tmpProperties;
                    if (!syncReport) {
                        // 当 syncReport = false 时，从同一个线程(reportCacheExecutor)中调用 properties.setProperty 和 properties.store，因此不需要深度复制
                        tmpProperties = properties;
                    } else {
                        // 使用 store 方法和 this.properties 的 setProperty 方法会在多线程环境下引起锁竞争，因此需要深度复制一个新的容器
                        tmpProperties = new Properties();
                        Set<Map.Entry<Object, Object>> entries = properties.entrySet();
                        for (Map.Entry<Object, Object> entry : entries) {
                            tmpProperties.setProperty((String) entry.getKey(), (String) entry.getValue());
                        }
                    }

                    try (FileOutputStream outputFile = new FileOutputStream(file)) {
                        tmpProperties.store(outputFile, "Dubbo metadataReport Cache");
                    }
                } finally {
                    lock.release();
                }
            }
        } catch (Throwable e) {
            if (version < lastCacheChanged.get()) {
                return;
            } else {
                reportCacheExecutor.execute(new SaveProperties(lastCacheChanged.incrementAndGet()));
            }
            logger.warn(COMMON_UNEXPECTED_EXCEPTION, "", "",
                "Failed to save service store file, cause: " + e.getMessage(), e);
        }
    }

    /**
     * 加载本地磁盘缓存文件中的属性
     */
    void loadProperties() {
        if (file != null && file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                properties.load(in);
                if (logger.isInfoEnabled()) {
                    logger.info("Load service store file " + file + ", data: " + properties);
                }
            } catch (Throwable e) {
                logger.warn(COMMON_UNEXPECTED_EXCEPTION, "", "", "Failed to load service store file" + file, e);
            }
        }
    }

    /**
     * 将元数据信息保存到本地文件中
     *
     * @param metadataIdentifier 元数据标识符，用于唯一标识元数据信息
     * @param value              要保存的元数据信息的字符串表示
     * @param add                是否添加元数据信息，如果为 true，则添加；否则，移除
     * @param sync               是否同步保存，如果为 true，则同步保存；否则，异步保存
     */
    private void saveProperties(MetadataIdentifier metadataIdentifier, String value, boolean add, boolean sync) {
        if (file == null) {
            return;
        }

        try {
            if (add) {
                // 添加元数据信息到 properties 中
                properties.setProperty(metadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), value);
            } else {
                // 移除指定的元数据信息
                properties.remove(metadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
            }
            // 更新缓存变化版本号
            long version = lastCacheChanged.incrementAndGet();
            if (sync) {
                // 同步保存属性到文件
                new SaveProperties(version).run();
            } else {
                // 异步执行保存属性到文件任务
                reportCacheExecutor.execute(new SaveProperties(version));
            }

        } catch (Throwable t) {
            logger.warn(COMMON_UNEXPECTED_EXCEPTION, "", "", t.getMessage(), t);
        }
    }

    @Override
    public String toString() {
        return getUrl().toString();
    }

    /**
     * 内部类，实现了 `Runnable` 接口，用于保存属性到本地文件
     */
    private class SaveProperties implements Runnable {
        private long version;

        private SaveProperties(long version) {
            this.version = version;
        }

        @Override
        public void run() {
            doSaveProperties(version);
        }
    }

    /**
     * 保存属性到本地磁盘缓存文件中
     *
     * @param providerMetadataIdentifier 提供者元数据标识符
     * @param serviceDefinition          要存储的服务定义
     */
    @Override
    public void storeProviderMetadata(MetadataIdentifier providerMetadataIdentifier, ServiceDefinition serviceDefinition) {
        if (syncReport) {
            storeProviderMetadataTask(providerMetadataIdentifier, serviceDefinition);
        } else {
            reportCacheExecutor.execute(() -> storeProviderMetadataTask(providerMetadataIdentifier, serviceDefinition));
        }
    }

    /**
     * 异步任务：存储服务提供者元数据的任务
     *
     * @param providerMetadataIdentifier 提供者元数据的标识符
     * @param serviceDefinition          服务定义对象
     */
    private void storeProviderMetadataTask(MetadataIdentifier providerMetadataIdentifier, ServiceDefinition serviceDefinition) {
        // 将元数据事件转换为服务订阅事件
        MetadataEvent metadataEvent = MetadataEvent.toServiceSubscribeEvent(applicationModel, providerMetadataIdentifier.getUniqueServiceName());

        // 发布元数据事件到指标事件总线，执行回调任务
        MetricsEventBus.post(
            metadataEvent,
            () -> {
                boolean result = true;
                try {
                    // 记录日志：存储服务提供者元数据
                    if (logger.isInfoEnabled()) {
                        logger.info("store provider metadata. Identifier : " + providerMetadataIdentifier
                            + "; definition: " + serviceDefinition);
                    }

                    // 将服务定义对象放入所有元数据报告的缓存中，移除失败的报告
                    allMetadataReports.put(providerMetadataIdentifier, serviceDefinition);
                    failedReports.remove(providerMetadataIdentifier);

                    // 将服务定义对象转换为 JSON 字符串并存储到元数据存储中
                    String data = JsonUtils.toJson(serviceDefinition);
                    doStoreProviderMetadata(providerMetadataIdentifier, data);

                    // 保存属性变更到本地属性缓存
                    saveProperties(providerMetadataIdentifier, data, true, !syncReport);
                } catch (Exception e) {
                    // 如果存储失败，记录错误日志，加入失败的报告列表，并启动重试任务
                    failedReports.put(providerMetadataIdentifier, serviceDefinition);
                    metadataReportRetry.startRetryTask();
                    logger.error(PROXY_FAILED_EXPORT_SERVICE, "", "",
                        "Failed to put provider metadata " + providerMetadataIdentifier + " in  "
                            + serviceDefinition + ", cause: " + e.getMessage(),
                        e);
                    result = false;
                }
                return result;
            },
            aBoolean -> aBoolean);
    }

    /**
     * 存储消费者元数据
     * 如果同步报告开关打开，则直接调用同步方法存储；否则，通过线程池异步执行存储任务
     *
     * @param consumerMetadataIdentifier 消费者元数据的标识符
     * @param serviceParameterMap        服务参数映射表
     */
    @Override
    public void storeConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, Map<String, String> serviceParameterMap) {
        if (syncReport) {
            storeConsumerMetadataTask(consumerMetadataIdentifier, serviceParameterMap);
        } else {
            reportCacheExecutor.execute(() -> storeConsumerMetadataTask(consumerMetadataIdentifier, serviceParameterMap));
        }
    }

    /**
     * 异步任务：存储消费者元数据的任务
     *
     * @param consumerMetadataIdentifier 消费者元数据的标识符
     * @param serviceParameterMap        服务参数映射表
     */
    protected void storeConsumerMetadataTask(MetadataIdentifier consumerMetadataIdentifier, Map<String, String> serviceParameterMap) {
        try {
            // 记录日志：存储消费者元数据
            if (logger.isInfoEnabled()) {
                logger.info("store consumer metadata. Identifier : " + consumerMetadataIdentifier + "; definition: "
                    + serviceParameterMap);
            }

            // 将服务参数映射表放入所有元数据报告的缓存中，移除失败的报告
            allMetadataReports.put(consumerMetadataIdentifier, serviceParameterMap);
            failedReports.remove(consumerMetadataIdentifier);

            // 将服务参数映射表转换为 JSON 字符串并存储到元数据存储中
            String data = JsonUtils.toJson(serviceParameterMap);
            doStoreConsumerMetadata(consumerMetadataIdentifier, data);

            // 保存属性变更到本地属性缓存
            saveProperties(consumerMetadataIdentifier, data, true, !syncReport);
        } catch (Exception e) {
            // 如果存储失败，记录错误日志，加入失败的报告列表，并启动重试任务
            failedReports.put(consumerMetadataIdentifier, serviceParameterMap);
            metadataReportRetry.startRetryTask();
            logger.error(
                PROXY_FAILED_EXPORT_SERVICE,
                "",
                "",
                "Failed to put consumer metadata " + consumerMetadataIdentifier + ";  " + serviceParameterMap
                    + ", cause: " + e.getMessage(),
                e);
        }
    }

    /**
     * 销毁方法，用于释放资源和关闭相关任务调度器
     */
    @Override
    public void destroy() {
        // 关闭报告缓存执行器
        if (reportCacheExecutor != null) {
            reportCacheExecutor.shutdown();
        }

        // 关闭报告定时调度器
        if (reportTimerScheduler != null) {
            reportTimerScheduler.shutdown();
        }

        // 销毁元数据报告重试管理器，并置空引用
        if (metadataReportRetry != null) {
            metadataReportRetry.destroy();
            metadataReportRetry = null;
        }
    }

    /**
     * 保存服务元数据。根据同步设置，同步执行或通过报告缓存执行保存操作
     *
     * @param metadataIdentifier 服务元数据标识符
     * @param url                服务的URL
     */
    @Override
    public void saveServiceMetadata(ServiceMetadataIdentifier metadataIdentifier, URL url) {
        if (syncReport) {
            doSaveMetadata(metadataIdentifier, url);
        } else {
            reportCacheExecutor.execute(() -> doSaveMetadata(metadataIdentifier, url));
        }
    }

    /**
     * 移除服务元数据。根据同步设置，同步执行或通过报告缓存执行移除操作
     *
     * @param metadataIdentifier 服务元数据标识符
     */
    @Override
    public void removeServiceMetadata(ServiceMetadataIdentifier metadataIdentifier) {
        if (syncReport) {
            doRemoveMetadata(metadataIdentifier);
        } else {
            reportCacheExecutor.execute(() -> doRemoveMetadata(metadataIdentifier));
        }
    }

    /**
     * 获取导出的URL列表。如果未能获取，则回退到本地缓存
     *
     * @param metadataIdentifier 服务元数据标识符
     * @return 导出的URL列表
     */
    @Override
    public List<String> getExportedURLs(ServiceMetadataIdentifier metadataIdentifier) {
        // TODO 回退到本地缓存
        return doGetExportedURLs(metadataIdentifier);
    }

    /**
     * 存储订阅的数据。如果同步报告开启，则直接存储订阅数据；否则，将异步执行存储操作
     *
     * @param subscriberMetadataIdentifier 订阅元数据标识符
     * @param urls                         订阅的URL集合
     */
    @Override
    public void saveSubscribedData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, Set<String> urls) {
        if (syncReport) {
            doSaveSubscriberData(subscriberMetadataIdentifier, JsonUtils.toJson(urls));
        } else {
            reportCacheExecutor.execute(() -> doSaveSubscriberData(subscriberMetadataIdentifier, JsonUtils.toJson(urls)));
        }
    }

    /**
     * 获取订阅的URL列表
     *
     * @param subscriberMetadataIdentifier 订阅元数据标识符
     * @return 订阅的URL列表
     */
    @Override
    public List<String> getSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier) {
        String content = doGetSubscribedURLs(subscriberMetadataIdentifier);
        return JsonUtils.toJavaList(content, String.class);
    }

    /**
     * 获取URL的协议
     *
     * @param url URL对象
     * @return URL的协议
     */
    String getProtocol(URL url) {
        String protocol = url.getSide();
        protocol = protocol == null ? url.getProtocol() : protocol;
        return protocol;
    }

    /**
     * 判断是否需要重试处理元数据集合
     *
     * @return 如果需要继续重试，则返回true；否则返回false
     */
    public boolean retry() {
        return doHandleMetadataCollection(failedReports);
    }

    /**
     * 指示是否应报告定义
     *
     * @return 如果应报告定义，则返回true；否则返回false
     */
    @Override
    public boolean shouldReportDefinition() {
        return reportDefinition;
    }

    /**
     * 指示是否应报告元数据
     *
     * @return 如果应报告元数据，则返回true；否则返回false
     */
    @Override
    public boolean shouldReportMetadata() {
        return reportMetadata;
    }

    /**
     * 处理元数据集合的方法，根据元数据的侧边（提供者或消费者）将其存储到相应的位置
     *
     * @param metadataMap 元数据映射，包含要处理的元数据标识符和相应的对象
     * @return 如果处理完毕后需要继续重试，则返回true；否则返回false
     */
    private boolean doHandleMetadataCollection(Map<MetadataIdentifier, Object> metadataMap) {
        if (metadataMap.isEmpty()) {
            return true;
        }
        Iterator<Map.Entry<MetadataIdentifier, Object>> iterable = metadataMap.entrySet().iterator();
        while (iterable.hasNext()) {
            Map.Entry<MetadataIdentifier, Object> item = iterable.next();
            if (PROVIDER_SIDE.equals(item.getKey().getSide())) {
                // 如果是提供者侧的元数据，则存储为完整的服务定义对象
                this.storeProviderMetadata(item.getKey(), (FullServiceDefinition) item.getValue());
            } else if (CONSUMER_SIDE.equals(item.getKey().getSide())) {
                // 如果是消费者侧的元数据，则存储为参数映射
                this.storeConsumerMetadata(item.getKey(), (Map) item.getValue());
            }
        }
        return false;
    }

    /**
     * 用于单元测试的方法，不是私有方法
     * 发布所有元数据到相应的处理方法
     */
    void publishAll() {
        logger.info("start to publish all metadata.");
        this.doHandleMetadataCollection(allMetadataReports);
    }

    /**
     * 计算一个起始时间，用于设置定时任务的启动时间
     * 时间计算逻辑包括：
     * 1. 获取当前时间的毫秒数
     * 2. 将日历设置为当天的午夜（00:00:00.000）
     * 3. 计算当前时间到午夜的毫秒数差
     * 4. 加上一定的偏移量，包括四小时的一半和一个四小时内的随机毫秒数
     *
     * @return 计算得到的起始时间
     */
    long calculateStartTime() {
        Calendar calendar = Calendar.getInstance();
        long nowMill = calendar.getTimeInMillis();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long subtract = calendar.getTimeInMillis() + ONE_DAY_IN_MILLISECONDS - nowMill;
        return subtract
            + (FOUR_HOURS_IN_MILLISECONDS / 2)
            + ThreadLocalRandom.current().nextInt(FOUR_HOURS_IN_MILLISECONDS);
    }

    /**
     * MetadataReportRetry 类用于处理元数据报告的重试机制
     */
    class MetadataReportRetry {
        protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

        /**
         * 用于执行定时重试任务的调度执行器服务
         */
        final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(0, new NamedThreadFactory("DubboMetadataReportRetryTimer", true));

        /**
         * 用于取消重试任务的计划执行句柄
         */
        volatile ScheduledFuture retryScheduledFuture;

        /**
         * 重试次数计数器，用于记录已经进行的重试次数
         */
        final AtomicInteger retryCounter = new AtomicInteger(0);

        /**
         * 重试任务的执行周期，以毫秒为单位
         */
        long retryPeriod;

        /**
         * 当没有失败报告时，等待多少次运行重试任务
         */
        int retryTimesIfNonFail = 600;

        /**
         * 重试限制次数，达到此次数后不再继续重试
         */
        int retryLimit;

        /**
         * 构造函数，初始化重试次数和重试周期
         *
         * @param retryTimes  重试次数限制
         * @param retryPeriod 重试周期（毫秒）
         */
        public MetadataReportRetry(int retryTimes, int retryPeriod) {
            this.retryPeriod = retryPeriod;
            this.retryLimit = retryTimes;
        }

        /**
         * 启动重试任务，如果未启动则执行定时重试
         */
        void startRetryTask() {
            if (retryScheduledFuture == null) {
                synchronized (retryCounter) {
                    if (retryScheduledFuture == null) {
                        retryScheduledFuture = retryExecutor.scheduleWithFixedDelay(
                            () -> {
                                // 检查并连接到元数据
                                try {
                                    int times = retryCounter.incrementAndGet();
                                    logger.info("start to retry task for metadata report. retry times:" + times);

                                    // 执行重试操作，如果无失败报告并且超过指定重试次数，则取消重试任务
                                    if (retry() && times > retryTimesIfNonFail) {
                                        cancelRetryTask();
                                    }

                                    // 如果超过重试限制次数，则取消重试任务
                                    if (times > retryLimit) {
                                        cancelRetryTask();
                                    }
                                } catch (Throwable t) { // 防御性容错处理
                                    logger.error(
                                        COMMON_UNEXPECTED_EXCEPTION,
                                        "",
                                        "",
                                        "Unexpected error occur at failed retry, cause: " + t.getMessage(),
                                        t);
                                }
                            },
                            500,
                            retryPeriod,
                            TimeUnit.MILLISECONDS);
                    }
                }
            }
        }

        /**
         * 取消重试任务。如果存在已计划的任务，则取消并关闭重试执行器
         */
        void cancelRetryTask() {
            if (retryScheduledFuture != null) {
                retryScheduledFuture.cancel(false);
            }
            retryExecutor.shutdown();
        }

        /**
         * 销毁操作。调用取消重试任务方法以确保所有任务被取消
         */
        void destroy() {
            cancelRetryTask();
        }

        /**
         * 获取重试执行器实例，仅用于测试目的
         *
         * @deprecated 仅用于测试
         */
        @Deprecated
        ScheduledExecutorService getRetryExecutor() {
            return retryExecutor;
        }
    }

    /**
     * 将订阅者数据保存到持久存储中。如果 URL 列表为空，则直接返回
     * 对 URL 列表进行编码后保存。
     *
     * @param subscriberMetadataIdentifier 订阅者元数据标识
     * @param urls                         URL 列表
     */
    private void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, List<String> urls) {
        if (CollectionUtils.isEmpty(urls)) {
            return;
        }
        List<String> encodedUrlList = new ArrayList<>(urls.size());
        for (String url : urls) {
            encodedUrlList.add(URL.encode(url));
        }
        doSaveSubscriberData(subscriberMetadataIdentifier, encodedUrlList);
    }

    /**
     * 存储提供者元数据信息的抽象方法。由子类实现具体存储逻辑
     *
     * @param providerMetadataIdentifier 提供者元数据标识
     * @param serviceDefinitions         服务定义信息字符串
     */
    protected abstract void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions);

    /**
     * 存储消费者元数据信息的抽象方法。由子类实现具体存储逻辑
     *
     * @param consumerMetadataIdentifier 消费者元数据标识
     * @param serviceParameterString     服务参数字符串
     */
    protected abstract void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String serviceParameterString);

    /**
     * 存储服务元数据信息的抽象方法。由子类实现具体存储逻辑
     *
     * @param metadataIdentifier 服务元数据标识
     * @param url                URL 对象
     */
    protected abstract void doSaveMetadata(ServiceMetadataIdentifier metadataIdentifier, URL url);

    /**
     * 删除服务元数据信息的抽象方法。由子类实现具体删除逻辑
     *
     * @param metadataIdentifier 服务元数据标识
     */
    protected abstract void doRemoveMetadata(ServiceMetadataIdentifier metadataIdentifier);

    /**
     * 获取导出的 URL 列表的抽象方法。由子类实现具体获取逻辑
     *
     * @param metadataIdentifier 服务元数据标识
     * @return 导出的 URL 列表
     */
    protected abstract List<String> doGetExportedURLs(ServiceMetadataIdentifier metadataIdentifier);

    /**
     * 存储订阅者数据的抽象方法。由子类实现具体存储逻辑
     *
     * @param subscriberMetadataIdentifier 订阅者元数据标识
     * @param urlListStr                   URL 列表的 JSON 字符串形式
     */
    protected abstract void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, String urlListStr);

    /**
     * 获取订阅的 URL 列表的抽象方法。由子类实现具体获取逻辑
     *
     * @param subscriberMetadataIdentifier 订阅者元数据标识
     * @return 订阅的 URL 列表的 JSON 字符串形式
     */
    protected abstract String doGetSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier);

    /**
     * 获取报告缓存执行器的方法。仅供单元测试使用
     *
     * @return 报告缓存执行器
     * @deprecated 仅供单元测试使用
     */
    @Deprecated
    protected ExecutorService getReportCacheExecutor() {
        return reportCacheExecutor;
    }

    /**
     * 获取元数据报告重试管理器的方法。仅供单元测试使用
     *
     * @return 元数据报告重试管理器
     * @deprecated 仅供单元测试使用
     */
    @Deprecated
    protected MetadataReportRetry getMetadataReportRetry() {
        return metadataReportRetry;
    }
}
