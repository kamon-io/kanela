/*
 * =========================================================================================
 * Copyright © 2013-2025 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kanela.agent.bytebuddy;

import java.util.List;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher.Junction;

public class ClassPrefixMatcher extends Junction.AbstractBase<TypeDescription> {

  private final List<String> prefixes;

  public ClassPrefixMatcher(List<String> prefixes) {
    this.prefixes = prefixes;
  }

  @Override
  public boolean matches(TypeDescription target) {
    return this.prefixes.stream().anyMatch(prefix -> target.getName().startsWith(prefix));
  }

  public String toString() {
    return "hasPrefix(" + this.prefixes + ")";
  }

  public static ClassPrefixMatcher classPrefix(List<String> prefixes) {
    return new ClassPrefixMatcher(prefixes);
  }
}
