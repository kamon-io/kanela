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

package kanela.agent.util.banner;

import kanela.agent.util.AnsiColor;
import kanela.agent.util.BuildInfo;
import kanela.agent.util.conf.KanelaConfiguration;
import lombok.var;
import lombok.val;

public class KanelaBanner {

    private static final String[] BANNER = {"",
            " _  __                _        ______",
            "| |/ /               | |       \\ \\ \\ \\",
            "| ' / __ _ _ __   ___| | __ _   \\ \\ \\ \\",
            "|  < / _` | '_ \\ / _ \\ |/ _` |   ) ) ) )",
            "| . \\ (_| | | | |  __/ | (_| |  / / / /",
            "|_|\\_\\__,_|_| |_|\\___|_|\\__,_| /_/_/_/",
            "                              ",
            "==============================" };

    private static final String KANELA = ":: The Kamon Agent ::";

    private static final int STRAP_LINE_SIZE = 10;

    public static void show(KanelaConfiguration configuration) {
        if (configuration.getShowBanner()) {
            val printStream = System.out;

            for (String line : BANNER) {
                System.out.println(line);
            }

            var version = BuildInfo.version();

            version =  (version == null ? "" : " (v" + version + ")");
            val padding = new StringBuilder();

            while (padding.length() < STRAP_LINE_SIZE - (version.length() + KANELA.length())) {
                padding.append(" ");
            }

            printStream.println(AnsiColor.ParseColors(":green,n:" + KANELA + padding + version));
            printStream.println();
        }
    }
}
