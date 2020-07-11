/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
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


package kanela.agent.api.instrumentation.classloader;

import io.vavr.Tuple;
import io.vavr.control.Option;
import lombok.Value;

import java.util.*;

@Value
public class ClassRefiner {
    String target;
    Map<String, Set<String>> methods;
    Map<String, Option<Object>> fields;

    private ClassRefiner(Builder builder) {
        this.target= builder.target;
        this.fields = builder.fields;
        this.methods = builder.methods;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String target;
        private final Map<String, Option<Object>> fields = new HashMap<>();
        private final Map<String, Set<String>> methods = new HashMap<>();

        @Deprecated
        public Builder mustContains(String clazz) {
            this.target = clazz;
            return this;
        }

        public Builder mustContain(String clazz) {
            this.target = clazz;
            return this;
        }

        public Builder withFields(String... fields) {
            Arrays.asList(fields).forEach(k -> withFieldAndValue(k, null));
            return this;
        }

        public Builder withFieldAndValue(String field, Object value) {
            this.fields.put(field, Option.of(value));
            return this;
        }

        public Builder withMethod(String method, String... params) {
            this.methods.put(method, new HashSet<>(Arrays.asList(params)));
            return this;
        }

        public Builder withMethods(String... methods) {
            this.methods.putAll(io.vavr.collection.List.of(methods).toJavaMap(method -> Tuple.of(method, new HashSet<>())));
            return this;
        }

        public ClassRefiner build() {
            if(target == null) throw new RuntimeException("We must provide a target class.");
            return new ClassRefiner(this);
        }
    }
}