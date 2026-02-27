/*
 * Copyright 2025 BaseMetas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.basemetas.fileview.preview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 轮询策略配置类
 * 
 * 配置智能轮询的各种参数，支持动态调整
 * 
 * @author 夫子
 */
@Configuration
@ConfigurationProperties(prefix = "fileview.preview.polling")
public class PollingConfig {
    
    /**
     * 默认超时时间（秒）
     */
    private int defaultTimeout = 20;
    
    /**
     * 最大超时时间（秒）
     */
    private int maxTimeout = 100;
    
    /**
     * 默认检查间隔（毫秒）
     */
    private int defaultInterval = 1000;
    
    /**
     * 最小检查间隔（毫秒）
     */
    private int minInterval = 500;
    
    /**
     * 最大检查间隔（毫秒）
     */
    private int maxInterval = 5000;
    
    /**
     * 智能轮询策略配置
     */
    private SmartPollingStrategy smartStrategy = new SmartPollingStrategy();
    
    /**
     * 长轮询专用线程池配置
     */
    private ThreadPoolConfig threadPool = new ThreadPoolConfig();
    
    /**
     * 随机Jitter配置
     */
    private JitterConfig jitter = new JitterConfig();
    
    // Getters and Setters
    public int getDefaultTimeout() {
        return defaultTimeout;
    }
    
    public void setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }
    
    public int getMaxTimeout() {
        return maxTimeout;
    }
    
    public void setMaxTimeout(int maxTimeout) {
        this.maxTimeout = maxTimeout;
    }
    
    public int getDefaultInterval() {
        return defaultInterval;
    }
    
    public void setDefaultInterval(int defaultInterval) {
        this.defaultInterval = defaultInterval;
    }
    
    public int getMinInterval() {
        return minInterval;
    }
    
    public void setMinInterval(int minInterval) {
        this.minInterval = minInterval;
    }
    
    public int getMaxInterval() {
        return maxInterval;
    }
    
    public void setMaxInterval(int maxInterval) {
        this.maxInterval = maxInterval;
    }
    
    public SmartPollingStrategy getSmartStrategy() {
        return smartStrategy;
    }
    
    public void setSmartStrategy(SmartPollingStrategy smartStrategy) {
        this.smartStrategy = smartStrategy;
    }
    
    public ThreadPoolConfig getThreadPool() {
        return threadPool;
    }
    
    public void setThreadPool(ThreadPoolConfig threadPool) {
        this.threadPool = threadPool;
    }
    
    public JitterConfig getJitter() {
        return jitter;
    }
    
    public void setJitter(JitterConfig jitter) {
        this.jitter = jitter;
    }
    
    /**
     * 长轮询专用线程池配置
     */
    public static class ThreadPoolConfig {
        /**
         * 核心线程数
         */
        private int corePoolSize = 50;
        
        /**
         * 最大线程数
         */
        private int maxPoolSize = 200;
        
        /**
         * 队列容量
         */
        private int queueCapacity = 500;
        
        /**
         * 空闲线程存活时间（秒）
         */
        private int keepAliveSeconds = 60;
        
        /**
         * 线程名称前缀
         */
        private String threadNamePrefix = "long-polling-";
        
        // Getters and Setters
        public int getCorePoolSize() {
            return corePoolSize;
        }
        
        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }
        
        public int getMaxPoolSize() {
            return maxPoolSize;
        }
        
        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }
        
        public int getQueueCapacity() {
            return queueCapacity;
        }
        
        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
        
        public int getKeepAliveSeconds() {
            return keepAliveSeconds;
        }
        
        public void setKeepAliveSeconds(int keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }
        
        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }
        
        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }
    }
    
    /**
     * 随机Jitter配置
     */
    public static class JitterConfig {
        /**
         * 是否启用Jitter
         */
        private boolean enabled = true;
        
        /**
         * 最小因子（例如0.8表示80%）
         */
        private double minFactor = 0.8;
        
        /**
         * 最大因子（例如1.2表示120%）
         */
        private double maxFactor = 1.2;
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public double getMinFactor() {
            return minFactor;
        }
        
        public void setMinFactor(double minFactor) {
            this.minFactor = minFactor;
        }
        
        public double getMaxFactor() {
            return maxFactor;
        }
        
        public void setMaxFactor(double maxFactor) {
            this.maxFactor = maxFactor;
        }
    }
    
    /**
     * 智能轮询策略配置
     */
    public static class SmartPollingStrategy {
        /**
         * 第一阶段尝试次数（快速轮询）
         */
        private int phase1Attempts = 10;
        
        /**
         * 第一阶段间隔（毫秒）
         */
        private int phase1Interval = 1000;
        
        /**
         * 第二阶段尝试次数（中等轮询）
         */
        private int phase2Attempts = 20;
        
        /**
         * 第二阶段间隔（毫秒）
         */
        private int phase2Interval = 2000;
        
        /**
         * 第三阶段间隔（毫秒）
         */
        private int phase3Interval = 5000;
        
        // Getters and Setters
        public int getPhase1Attempts() {
            return phase1Attempts;
        }
        
        public void setPhase1Attempts(int phase1Attempts) {
            this.phase1Attempts = phase1Attempts;
        }
        
        public int getPhase1Interval() {
            return phase1Interval;
        }
        
        public void setPhase1Interval(int phase1Interval) {
            this.phase1Interval = phase1Interval;
        }
        
        public int getPhase2Attempts() {
            return phase2Attempts;
        }
        
        public void setPhase2Attempts(int phase2Attempts) {
            this.phase2Attempts = phase2Attempts;
        }
        
        public int getPhase2Interval() {
            return phase2Interval;
        }
        
        public void setPhase2Interval(int phase2Interval) {
            this.phase2Interval = phase2Interval;
        }
        
        public int getPhase3Interval() {
            return phase3Interval;
        }
        
        public void setPhase3Interval(int phase3Interval) {
            this.phase3Interval = phase3Interval;
        }
    }
}