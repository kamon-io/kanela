package kanela.agent.util;

import kanela.agent.api.instrumentation.listener.InstrumentationRegistryListener;

import java.util.List;
import java.util.Map;

public class KanelaInformationProvider {

    public static Map<String, Map<String, List<String>>> getKanelaModulesInfo() {
        return InstrumentationRegistryListener.instance().getRecorded();
    }

    public static Map<String, List<Throwable>> getKanelaErrors() {
        return InstrumentationRegistryListener.instance().getErrors();
    }
}