package kamon.agent;


import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AgentLogger {

    private static final String KAMON_AGENT_LOGBACK = "kamon-agent-logback.xml";
    private static final String LOGBACK_CONFIGURATION_FILE= "logback.configurationFile";

    public static Logger create(String name) { return withLogbackConfiguration(() -> org.slf4j.LoggerFactory.getLogger(name));}
    public static Logger create(Class clazz) { return withLogbackConfiguration(() -> org.slf4j.LoggerFactory.getLogger(clazz));}

    private static Logger withLogbackConfiguration(Supplier<Logger> loggerFactory){
        final Map<String,String> systemProperties = new HashMap<>();

        try {

            clearSystemProperty(LOGBACK_CONFIGURATION_FILE, systemProperties);
            System.setProperty(LOGBACK_CONFIGURATION_FILE, KAMON_AGENT_LOGBACK);

            return loggerFactory.get();

        } finally {
            System.getProperties().remove(LOGBACK_CONFIGURATION_FILE);
            systemProperties.forEach(System::setProperty);
        }
    }

    private static void clearSystemProperty(String systemProperty, Map<String, String> systemProperties) {
        final String old = System.clearProperty(systemProperty);
        if(old != null) {
            systemProperties.put(systemProperty, old);
        }
    }

}
