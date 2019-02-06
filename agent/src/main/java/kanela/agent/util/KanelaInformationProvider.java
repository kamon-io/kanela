package kanela.agent.util;

import com.grack.nanojson.JsonWriter;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import kanela.agent.api.instrumentation.listener.InstrumentationRegistryListener;
import kanela.agent.util.conf.KanelaConfiguration;
import lombok.val;

import java.util.Map;

public class KanelaInformationProvider {

    public static final KanelaInformationProvider MODULE$ = new KanelaInformationProvider();
    private static final InstrumentationRegistryListener registryListener = InstrumentationRegistryListener.instance();

    public Boolean isActive() {
        return true;
    }

    public java.util.Map<String, String> modules() {
        return registryListener.getModulesConfiguration()
                .mapValues(KanelaInformationProvider::moduleJson).toJavaMap();
    }

    public Map<String, java.util.List<Throwable>> errors() {
        return registryListener.getErrors().mapValues(List::asJava).toJavaMap();
    }

    private static String moduleJson(KanelaConfiguration.ModuleConfiguration module) {
        val instrumentations = JsonWriter.string().object()
                .value("description", module.getName())
                .value("enabled", module.isEnabled())
                .value("active", registryListener.isModuleActive(module.getKey()))
                .value("order", module.getOrder())
                .array("instrumentations");
        return InstrumentationRegistryListener.instance().getModuleTransformers()
                .getOrElse(module.getKey(), HashMap.empty()).keySet()
                .foldLeft(instrumentations, (inst, i) -> inst.value(i.getInstrumentationName())).end().end().done();
    }

}