package kamon.akka.instrumentation.mixin

import akka.kamon.instrumentation.advisor.LookupDataAware.LookupData

class LookupDataAwareMixin extends LookupDataAware

trait LookupDataAware {
  @volatile var lookupData: LookupData = _
}
