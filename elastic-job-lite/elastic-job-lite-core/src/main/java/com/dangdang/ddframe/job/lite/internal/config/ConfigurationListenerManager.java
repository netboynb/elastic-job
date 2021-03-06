/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.job.lite.internal.config;

import com.dangdang.ddframe.job.event.JobEventBus;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.internal.listener.AbstractJobListener;
import com.dangdang.ddframe.job.lite.internal.listener.AbstractListenerManager;
import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.dangdang.ddframe.job.lite.internal.schedule.JobScheduleController;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;

/**
 * 配置文件监听管理器.
 * 
 * @author caohao
 * @author zhangliang
 */
public class ConfigurationListenerManager extends AbstractListenerManager {
    
    private final ConfigurationNode configNode;
    
    private final String jobName;
    
    private final JobEventBus jobEventBus;
    
    public ConfigurationListenerManager(final CoordinatorRegistryCenter regCenter, final String jobName) {
        super(regCenter, jobName);
        this.jobName = jobName;
        configNode = new ConfigurationNode(jobName);
        jobEventBus = JobEventBus.getInstance();
    }
    
    @Override
    public void start() {
        addDataListener(new CronSettingAndJobEventChangedJobListener());
    }
    
    class CronSettingAndJobEventChangedJobListener extends AbstractJobListener {
        
        @Override
        protected void dataChanged(final CuratorFramework client, final TreeCacheEvent event, final String path) {
            if (configNode.isConfigPath(path) && Type.NODE_UPDATED == event.getType()) {
                JobScheduleController jobScheduler = JobRegistry.getInstance().getJobScheduleController(jobName);
                if (null == jobScheduler) {
                    return;
                }
                LiteJobConfiguration liteJobConfiguration = LiteJobConfigurationGsonFactory.fromJson(new String(event.getData().getData()));
                jobEventBus.deregister(liteJobConfiguration.getJobName());
                jobEventBus.register(liteJobConfiguration.getJobName(), liteJobConfiguration.getTypeConfig().getCoreConfig().getJobEventConfigs().values());
                jobScheduler.rescheduleJob(liteJobConfiguration.getTypeConfig().getCoreConfig().getCron());
            }
        }
    }
}
