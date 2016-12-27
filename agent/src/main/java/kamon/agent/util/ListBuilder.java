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

package kamon.agent.util;

import javaslang.collection.List;

import java.util.ArrayList;

public class ListBuilder<T> {
    private java.util.List<T> list = new ArrayList<>();

    public ListBuilder add(T element) {
        this.list.add(element);
        return this;
    }

    public ListBuilder addAll(Iterable<T> elements) {
        this.list.addAll(List.ofAll(elements).toJavaList());
        return this;
    }

    public List<T> build() {
        return List.ofAll(this.list);
    }

    public static <T> ListBuilder<T> builder() {
        return new ListBuilder<>();
    }
}