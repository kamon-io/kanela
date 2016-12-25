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

package kamon.agent.util.banner;

import utils.AnsiColor;

import java.io.PrintStream;

public class KamonAgentBanner {

    private static final String[] BANNER = { "",
            " _  __                                                      _     __  _  _",
                    "| |/ /                                /\\                   | |   \\  \\  \\  \\",
                    "| ' / __ _ _ __ ___   ___  _ __      /  \\   __ _  ___ _ __ | |_   \\  \\  \\  \\",
                    "|  < / _` | '_ ` _ \\ / _ \\| '_ \\    / /\\ \\ / _` |/ _ \\ '_ \\| __|   )  )  )  )",
                    "| . \\ (_| | | | | | | (_) | | | |  / ____ \\ (_| |  __/ | | | |_   /  /  /  /",
                    "|_|\\_\\__,_|_| |_| |_|\\___/|_| |_| /_/    \\_\\__, |\\___|_| |_|\\__| / _/ _/ _/",
                    "                                            __/ |                ",
                    "========================================== |___/ =============== " };

    private static final String KAMON_AGENT = " :: Kamon Agent :: ";

    private static final int STRAP_LINE_SIZE = 42;

    public static void print(PrintStream printStream) {
        for (String line : BANNER) {
            printStream.println(line);
        }
        String version = "0.1.0";
        version = (version == null ? "" : " (v" + version + ")");
        String padding = "";

        while (padding.length() < STRAP_LINE_SIZE - (version.length() + KAMON_AGENT.length())) {
            padding += " ";
        }

        printStream.println(AnsiColor.ParseColors(":green,n:" + KAMON_AGENT + padding + version));
        printStream.println();
    }
}


