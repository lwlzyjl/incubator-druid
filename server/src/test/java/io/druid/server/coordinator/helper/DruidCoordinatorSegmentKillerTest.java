/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.druid.server.coordinator.helper;

import com.google.common.collect.ImmutableList;
import io.druid.client.indexing.IndexingServiceClient;
import io.druid.java.util.common.Intervals;
import io.druid.metadata.MetadataSegmentManager;
import io.druid.server.coordinator.TestDruidCoordinatorConfig;
import org.easymock.EasyMock;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 */
public class DruidCoordinatorSegmentKillerTest
{
  @Test
  public void testFindIntervalForKillTask()
  {
    testFindIntervalForKillTask(null, null);
    testFindIntervalForKillTask(ImmutableList.of(), null);

    testFindIntervalForKillTask(ImmutableList.of(Intervals.of("2014/2015")), Intervals.of("2014/2015"));

    testFindIntervalForKillTask(
        ImmutableList.of(Intervals.of("2014/2015"), Intervals.of("2016/2017")),
        Intervals.of("2014/2017")
    );

    testFindIntervalForKillTask(
        ImmutableList.of(Intervals.of("2014/2015"), Intervals.of("2015/2016")),
        Intervals.of("2014/2016")
    );

    testFindIntervalForKillTask(
        ImmutableList.of(Intervals.of("2015/2016"), Intervals.of("2014/2015")),
        Intervals.of("2014/2016")
    );

    testFindIntervalForKillTask(
        ImmutableList.of(Intervals.of("2015/2017"), Intervals.of("2014/2016")),
        Intervals.of("2014/2017")
    );

    testFindIntervalForKillTask(
        ImmutableList.of(
            Intervals.of("2015/2019"),
            Intervals.of("2014/2016"),
            Intervals.of("2018/2020")
        ),
        Intervals.of("2014/2020")
    );

    testFindIntervalForKillTask(
        ImmutableList.of(
            Intervals.of("2015/2019"),
            Intervals.of("2014/2016"),
            Intervals.of("2018/2020"),
            Intervals.of("2021/2022")
        ),
        Intervals.of("2014/2022")
    );
  }

  private void testFindIntervalForKillTask(List<Interval> segmentManagerResult, Interval expected)
  {
    MetadataSegmentManager segmentManager = EasyMock.createMock(MetadataSegmentManager.class);
    EasyMock.expect(
        segmentManager.getUnusedSegmentIntervals(
            EasyMock.anyString(),
            EasyMock.anyObject(Interval.class),
            EasyMock.anyInt()
        )
    ).andReturn(
        segmentManagerResult
    );
    EasyMock.replay(segmentManager);
    IndexingServiceClient indexingServiceClient = EasyMock.createMock(IndexingServiceClient.class);

    DruidCoordinatorSegmentKiller coordinatorSegmentKiller = new DruidCoordinatorSegmentKiller(
        segmentManager,
        indexingServiceClient,
        new TestDruidCoordinatorConfig(
            null,
            null,
            Duration.parse("PT76400S"),
            new Duration(1),
            Duration.parse("PT86400S"),
            Duration.parse("PT86400S"),
            1000,
            null,
            false,
            false,
            Duration.ZERO
        )
    );

    Assert.assertEquals(
        expected,
        coordinatorSegmentKiller.findIntervalForKillTask("test", 10000)
    );
  }
}
