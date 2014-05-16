/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.bluespar.kato.deploy.aws.ops

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.autoscaling.AmazonAutoScaling
import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult
import com.amazonaws.services.autoscaling.model.TerminateInstanceInAutoScalingGroupRequest
import com.netflix.bluespar.kato.data.task.Task
import com.netflix.bluespar.kato.data.task.TaskRepository
import com.netflix.bluespar.kato.deploy.aws.description.TerminateInstanceAndDecrementAsgDescription
import com.netflix.bluespar.kato.security.aws.AmazonClientProvider
import com.netflix.bluespar.kato.security.aws.AmazonCredentials
import spock.lang.Specification

class TerminateInstanceAndDecrementAsgAtomicOperationUnitSpec extends Specification {
  def setupSpec() {
    TaskRepository.threadLocalTask.set(Mock(Task))
  }

  void "operation invokes update to autoscaling group"() {
    setup:
    def mockAutoScaling = Mock(AmazonAutoScaling)
    def mockAmazonClientProvider = Mock(AmazonClientProvider)
    mockAmazonClientProvider.getAutoScaling(_, _) >> mockAutoScaling
    def description = new TerminateInstanceAndDecrementAsgDescription(asgName: "myasg-stack-v000", region: "us-west-1", instance: "i-123456")
    description.credentials = new AmazonCredentials(Mock(AWSCredentials), "baz")
    def operation = new TerminateInstanceAndDecrementAsgAtomicOperation(description)
    operation.amazonClientProvider = mockAmazonClientProvider

    when:
    operation.operate([])

    then:
    1 * mockAutoScaling.describeAutoScalingGroups(_) >> { DescribeAutoScalingGroupsRequest request ->
      assert request.autoScalingGroupNames == ["myasg-stack-v000"]
      def mock = Mock(AutoScalingGroup)
      mock.getAutoScalingGroupName() >> "myasg-stack-v000"
      new DescribeAutoScalingGroupsResult().withAutoScalingGroups(mock)
    }
    1 * mockAutoScaling.terminateInstanceInAutoScalingGroup(_) >> { TerminateInstanceInAutoScalingGroupRequest request ->
      assert request.instanceId == "i-123456"
      assert request.shouldDecrementDesiredCapacity
    }
  }
}
