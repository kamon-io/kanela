/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
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

package akka.kamon.instrumentation

import akka.actor.{ Cell, ActorRef, ActorSystem }
import akka.routing.{ NoRouter, RoutedActorRef }
import kamon.Kamon
import kamon.akka.{ ActorMetrics, RouterMetrics }
import kamon.metric.Entity

case class CellInfo(entity: Entity, isRouter: Boolean, isRoutee: Boolean, isTracked: Boolean)

object CellInfo {

  def cellName(system: ActorSystem, ref: ActorRef): String =
    system.name + "/" + ref.path.elements.mkString("/")

  def cellInfoFor(cell: Cell, system: ActorSystem, ref: ActorRef, parent: ActorRef): CellInfo = {
    import kamon.metric.Entity
    def hasRouterProps(cell: Cell): Boolean = cell.props.deploy.routerConfig != NoRouter

    val pathString = ref.path.elements.mkString("/")
    val isRootSupervisor = pathString.length == 0 || pathString == "user" || pathString == "system"
    val isRouter = hasRouterProps(cell)
    val isRoutee = parent.isInstanceOf[RoutedActorRef]

    val name = if (isRoutee) cellName(system, parent) else cellName(system, ref)
    val category = if (isRouter || isRoutee) RouterMetrics.category else ActorMetrics.category
    val entity = Entity(name, category)
    val isTracked = !isRootSupervisor && Kamon.metrics.shouldTrack(entity)

    CellInfo(entity, isRouter, isRoutee, isTracked)
  }
}