/*
 * =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
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

package kamon.agent.util.matcher;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.bytebuddy.matcher.ElementMatcher;

@Value
@EqualsAndHashCode(callSuper = false)
public class ClassLoaderMatcher extends ElementMatcher.Junction.AbstractBase<ClassLoader>{

    String classLoaderName;

    public static ElementMatcher.Junction.AbstractBase<ClassLoader> withName(String name) {
        return new ClassLoaderMatcher(name);
    }

    public static ElementMatcher.Junction.AbstractBase<ClassLoader> isReflectionClassLoader() {
        return new ClassLoaderMatcher("sun.reflect.DelegatingClassLoader");
    }

    @Override
    public boolean matches(ClassLoader target) {
        return (target != null) && classLoaderName.equals(target.getClass().getName());
    }
}
