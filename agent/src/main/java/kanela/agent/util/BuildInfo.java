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

import io.vavr.control.Try;
import lombok.val;

import java.util.Properties;

public class BuildInfo {
    private static String version;
    private static String timestamp;

    static {
        val properties = loadProperties();
        version = properties.getProperty("version");
        timestamp  = properties.getProperty("timestamp");
    }

    public static String version() {
        return BuildInfo.version;
    }
    public static String timestamp() {
        return BuildInfo.timestamp;
    }

    private static Properties loadProperties() {
        return Try.of(() -> {
            val properties = new Properties();
            val is = BuildInfo.class.getResourceAsStream("/build-info.properties");
            properties.load(is);
            return properties;
        }).getOrElseThrow((cause) -> new RuntimeException("Error trying to read build-info.properties", cause));
    }
}
