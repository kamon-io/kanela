package kamon.akka.instrumentation.mixin

import akka.kamon.instrumentation.RouterMonitor

trait RouterInstrumentationAware {
  def routerInstrumentation: RouterMonitor
  def setRouterInstrumentation(ai: RouterMonitor): Unit
}

class RoutedActorCellInstrumentationMixin extends RouterInstrumentationAware {
    @volatile private var _ri: RouterMonitor = _

    def setRouterInstrumentation(ai: RouterMonitor): Unit = _ri = ai
    def routerInstrumentation: RouterMonitor = _ri
}
