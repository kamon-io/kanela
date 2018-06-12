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

package kanela.agent.bootstrap.log;

/**
 * Interface for accessing and manipulating the logger.
 *
 * @since 0.12
 */
public interface LoggerProvider {
    void error(String msg, Throwable t);
    void info(String msg);

    enum NoOp implements LoggerProvider {

        INSTANCE;

        public void error(String msg, Throwable t) {}
        public void info(String msg) {}
    }
}


