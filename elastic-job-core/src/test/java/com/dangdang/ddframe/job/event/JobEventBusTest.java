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

package com.dangdang.ddframe.job.event;

import com.dangdang.ddframe.job.event.JobEventBus.JobEventBusInstance;
import com.dangdang.ddframe.job.event.JobExecutionEvent.ExecutionSource;
import com.dangdang.ddframe.job.event.JobTraceEvent.LogLevel;
import com.dangdang.ddframe.job.event.fixture.JobEventCaller;
import com.dangdang.ddframe.job.event.fixture.TestJobEventConfiguration;
import com.dangdang.ddframe.job.event.fixture.TestJobEventListener;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.unitils.util.ReflectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public final class JobEventBusTest {
    
    @Mock
    private JobEventCaller jobEventCaller;
    
    private final JobEventBus jobEventBus = JobEventBus.getInstance();
    
    private final String jobName = "test_event_bus_job";
    
    @After
    public void tearDown() {
        jobEventBus.deregister(jobName);
        TestJobEventListener.reset();
    }
    
    @Test
    public void assertPostWithoutListenerRegistered() {
        jobEventBus.post(new JobTraceEvent(jobName, LogLevel.INFO, "ok"));
        jobEventBus.post(new JobExecutionEvent(jobName, ExecutionSource.NORMAL_TRIGGER, 0));
        verify(jobEventCaller, times(0)).call();
    }
    
    @Test
    public void assertPostWithListenerRegistered() throws InterruptedException {
        registerEventConfigs();
        jobEventBus.post(new JobTraceEvent(jobName, LogLevel.INFO, "ok"));
        jobEventBus.post(new JobExecutionEvent(jobName, ExecutionSource.NORMAL_TRIGGER, 0));
        while (!TestJobEventListener.isExecutionEventCalled() || !TestJobEventListener.isTraceEventCalled()) {
            Thread.sleep(100L);
        }
        verify(jobEventCaller, times(2)).call();
    }
    
    @Test
    public void assertPostWithListenerRegisteredTwice() throws InterruptedException {
        registerEventConfigs();
        registerEventConfigs();
        jobEventBus.post(new JobTraceEvent(jobName, LogLevel.INFO, "ok"));
        jobEventBus.post(new JobExecutionEvent(jobName, ExecutionSource.NORMAL_TRIGGER, 0));
        while (!TestJobEventListener.isExecutionEventCalled() || !TestJobEventListener.isTraceEventCalled()) {
            Thread.sleep(100L);
        }
        verify(jobEventCaller, times(2)).call();
    }
    
    @Test
    public void assertGetWorkQueueSize() {
        registerEventConfigs();
        assertThat(jobEventBus.getWorkQueueSize().size(), is(1));
        assertThat(jobEventBus.getWorkQueueSize().get(jobName), is(0));
    }
    
    @Test
    public void assertClearListeners() throws NoSuchFieldException {
        JobEventBusInstance jobEventBusInstance = mock(JobEventBusInstance.class);
        setItemMap(jobEventBusInstance);
        jobEventBus.clearListeners(jobName);
        verify(jobEventBusInstance).clearListeners();
    }
    
    @Test
    public void assertDeregister() throws InterruptedException, NoSuchFieldException {
        JobEventBusInstance jobEventBusInstance = mock(JobEventBusInstance.class);
        setItemMap(jobEventBusInstance);
        jobEventBus.deregister(jobName);
        verify(jobEventBusInstance).clearListeners();
    }
    
    @Test
    public void assertDeregisterWitAnotherJobName() throws InterruptedException, NoSuchFieldException {
        JobEventBusInstance jobEventBusInstance = mock(JobEventBusInstance.class);
        setItemMap(jobEventBusInstance);
        jobEventBus.deregister("anotherJob");
        verify(jobEventBusInstance, times(0)).clearListeners();
    }
    
    private void setItemMap(final JobEventBusInstance jobEventBusInstance) throws NoSuchFieldException {
        ConcurrentHashMap<String, JobEventBusInstance> itemMap = new ConcurrentHashMap<>();
        itemMap.put(jobName, jobEventBusInstance);
        ReflectionUtils.setFieldValue(jobEventBus, "itemMap", itemMap);
    }
    
    private void registerEventConfigs() {
        Map<String, JobEventConfiguration> jobEventConfigs = new LinkedHashMap<>(1, 1);
        TestJobEventConfiguration jobEventConfiguration = new TestJobEventConfiguration(jobEventCaller);
        jobEventConfigs.put("test", jobEventConfiguration);
        jobEventBus.register(jobName, jobEventConfigs.values());
    }
}
