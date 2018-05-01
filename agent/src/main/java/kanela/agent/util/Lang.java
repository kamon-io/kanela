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

package kanela.agent.util;

import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.experimental.var;
import lombok.val;

public class Lang {

    public static String getRunningJavaVersion() {
        val version = System.getProperty("java.version");
        var pos = version.indexOf('.');
        pos = version.indexOf('.', pos+1);
        return version.substring (0, pos);
    }

    public static Option<String> getRunningScalaVersion() {
        return Try.of(() -> {
            val props = new java.util.Properties();
            props.load(Lang.class.getResourceAsStream("/library.properties"));
            val version = props.getProperty("version.number");
            return version.substring(0, version.lastIndexOf("."));
        }).toOption();
    }
}
