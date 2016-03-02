package kamon.agent.api.banner;

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

    public static void printBanner(PrintStream printStream) {
        for (String line : BANNER) {
            printStream.println(line);
        }
        String version = "1.0";
        version = (version == null ? "" : " (v" + version + ")");
        String padding = "";
        while (padding.length() < STRAP_LINE_SIZE
                - (version.length() + KAMON_AGENT.length())) {
            padding += " ";
        }

        printStream.println(AnsiColor.ParseColors(":green,n:" + KAMON_AGENT + padding + version));
        printStream.println();
    }
}


