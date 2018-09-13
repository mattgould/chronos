package org.apache.mesos.chronos.scheduler.jobs

import com.codahale.metrics.MetricRegistry
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import org.apache.mesos.chronos.ChronosTestHelper._
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.mesos.MesosOfferReviver
import org.apache.mesos.chronos.scheduler.state.PersistenceStore
import org.joda.time._
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit

class TaskManagerSpec extends SpecificationWithJUnit with Mockito {
  "TaskManager" should {
    "Handle None job option in getTaskFromQueue" in {
      val mockJobGraph = mock[JobGraph]
      val mockPersistencStore: PersistenceStore = mock[PersistenceStore]

      val taskManager =
        new TaskManager(mock[ListeningScheduledExecutorService],
                        mockPersistencStore,
                        mockJobGraph,
                        null,
                        MockJobUtils.mockFullObserver,
                        mock[MetricRegistry],
                        makeConfig(),
                        mock[MesosOfferReviver])

      val job = ScheduleBasedJob("R/2012-01-01T00:00:01.000Z/PT1M",
                                 "test",
                                 "sample-command")

      mockJobGraph
        .lookupVertex("test")
        .returns(Some(job)) // so we can enqueue a job.
      taskManager.enqueue("ct:1420843781398:0:test:", highPriority = true)
      taskManager.queueContains("ct:1420843781398:0:test:") must_== true

      mockJobGraph.getJobForName("test").returns(None)

      taskManager.getTaskFromQueue must_== None
      taskManager.queueContains("ct:1420843781398:0:test:") must_== false
    }

    "Revive offers when adding a new task and --revive_offers_for_new_jobs is set" in {
      val mockJobGraph = mock[JobGraph]
      val mockPersistencStore: PersistenceStore = mock[PersistenceStore]
      val mockMesosOfferReviver = mock[MesosOfferReviver]
      val config = makeConfig("--revive_offers_for_new_jobs")

      val taskManager =
        new TaskManager(mock[ListeningScheduledExecutorService],
                        mockPersistencStore,
                        mockJobGraph,
                        null,
                        MockJobUtils.mockFullObserver,
                        mock[MetricRegistry],
                        config,
                        mockMesosOfferReviver)

      val job = ScheduleBasedJob("R/2012-01-01T00:00:01.000Z/PT1M",
                                 "test",
                                 "sample-command")
      mockJobGraph
        .lookupVertex("test")
        .returns(Some(job)) // so we can enqueue a job.

      taskManager.enqueue("ct:1420843781398:0:test:", highPriority = true)
      taskManager.queueContains("ct:1420843781398:0:test:") must_== true

      there was one(mockMesosOfferReviver).reviveOffers
    }

    "Don't revive offers when adding a new task and --revive_offers_for_new_jobs is not set" in {
      val mockJobGraph = mock[JobGraph]
      val mockPersistencStore: PersistenceStore = mock[PersistenceStore]
      val mockMesosOfferReviver = mock[MesosOfferReviver]
      val config = makeConfig()

      val taskManager =
        new TaskManager(mock[ListeningScheduledExecutorService],
                        mockPersistencStore,
                        mockJobGraph,
                        null,
                        MockJobUtils.mockFullObserver,
                        mock[MetricRegistry],
                        config,
                        mockMesosOfferReviver)

      val job = ScheduleBasedJob("R/2012-01-01T00:00:01.000Z/PT1M",
                                 "test",
                                 "sample-command")
      mockJobGraph
        .lookupVertex("test")
        .returns(Some(job)) // so we can enqueue a job.

      taskManager.enqueue("ct:1420843781398:0:test:", highPriority = true)
      taskManager.queueContains("ct:1420843781398:0:test:") must_== true

      there were noCallsTo(mockMesosOfferReviver)
    }

    "Add a new task" in {
      val mockJobGraph = mock[JobGraph]
      val mockPersistencStore: PersistenceStore = mock[PersistenceStore]
      val mockMesosOfferReviver = mock[MesosOfferReviver]
      val config = makeConfig()

      val taskManager =
        new TaskManager(mock[ListeningScheduledExecutorService],
                        mockPersistencStore,
                        mockJobGraph,
                        null,
                        MockJobUtils.mockFullObserver,
                        mock[MetricRegistry],
                        config,
                        mockMesosOfferReviver)

      taskManager.addTask("job", "slave", "task")

      taskManager.getRunningTaskCount("job") must_== 1
    }

    "Remove a task" in {
      val mockJobGraph = mock[JobGraph]
      val mockPersistencStore: PersistenceStore = mock[PersistenceStore]
      val mockMesosOfferReviver = mock[MesosOfferReviver]
      val config = makeConfig()

      val taskManager =
        new TaskManager(mock[ListeningScheduledExecutorService],
                        mockPersistencStore,
                        mockJobGraph,
                        null,
                        MockJobUtils.mockFullObserver,
                        mock[MetricRegistry],
                        config,
                        mockMesosOfferReviver)

      taskManager.addTask("job", "slave", "ct:1420843781398:0:job:")
      taskManager.getRunningTaskCount("job") must_== 1
      taskManager.removeTask("ct:1420843781398:0:job:")
      taskManager.getRunningTaskCount("job") must_== 0
    }
  }
}
