/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.servlet.instrumentation

import kamon.Kamon
import kamon.metric.EntitySnapshot

trait KamonSpec {

  lazy val collectionContext = Kamon.metrics.buildDefaultCollectionContext

  def takeSnapshotOf(name: String, category: String): EntitySnapshot = {
    val recorder = Kamon.metrics.find(name, category).get
    recorder.collect(collectionContext)
  }

  def takeSnapshotOf(name: String, category: String, tags: Map[String, String]): EntitySnapshot = {
    val recorder = Kamon.metrics.find(name, category, tags).get
    recorder.collect(collectionContext)
  }
}
