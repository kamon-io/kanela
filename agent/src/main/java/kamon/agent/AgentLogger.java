package kamon.agent;


import org.slf4j.Logger;

import java.util.function.Supplier;

public class AgentLogger {

    // set: logback.configurationFile=kamon-agent-logback.xml
    // desired: -Dkamon.agent.logback.configurationFile=agent/src/main/resources/kamon-agent-logback.xml
    public static Logger create(Supplier<Logger> loggerFactory) {
        String initLogbackConfigFile = System.getProperty("logback.configurationFile");
        String agentLogback = "kamon-agent-logback.xml";
        try {
            System.setProperty("logback.configurationFile", agentLogback);
            return loggerFactory.get();
        } finally {
            if (initLogbackConfigFile != null) {
                System.setProperty("logback.configurationFile", initLogbackConfigFile);
            }
        }
    }

    public static Logger create(String name) {
        return create(() -> org.slf4j.LoggerFactory.getLogger(name));
    }

    public static Logger create(Class clazz) {
        return create(() -> org.slf4j.LoggerFactory.getLogger(clazz));
    }
}
