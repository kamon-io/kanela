package kanela.agent.util;

import com.grack.nanojson.JsonStringWriter;
import com.grack.nanojson.JsonWriter;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import kanela.agent.api.instrumentation.TypeTransformation;
import kanela.agent.api.instrumentation.listener.InstrumentationRegistryListener;
import kanela.agent.util.conf.KanelaConfiguration;
import lombok.val;
import net.bytebuddy.description.type.TypeDescription;

import java.util.Map;

public class KanelaInformationProvider {

    public static Boolean isKanelaEnabled() {
        return true;
    }

    public static java.util.Map<String, String> getInstrumentationModulesInfo() {
        return InstrumentationRegistryListener.instance().getModulesConfiguration()
                .mapValues(KanelaInformationProvider::moduleJson).toJavaMap();
    }

    public static Map<String, java.util.List<Throwable>> getKanelaErrors() {
        return InstrumentationRegistryListener.instance().getErrors().mapValues(List::asJava).toJavaMap();
    }

    private static String moduleJson(KanelaConfiguration.ModuleConfiguration module) {
        val instrumentations = JsonWriter.string().object()
                .value("name", module.getName())
                .value("enabled", module.isEnabled())
                .value("order", module.getOrder())
                .array("instrumentations");
        return InstrumentationRegistryListener.instance().getModuleTransformers()
                .getOrElse(module.getKey(), List.empty())
                .foldLeft(instrumentations, (inst, i) -> addInstrumentationJson(inst.object(), i)).end().end().done();
    }

    private static JsonStringWriter addInstrumentationJson(JsonStringWriter object, Tuple2<TypeTransformation, List<TypeDescription>> instrumentation) {
        val types = object
                .value("name", instrumentation._1.getInstrumentationName())
                .array("types");
        return instrumentation._2.foldLeft(types, (ts, t) -> ts.value(t.getCanonicalName())).end().end();
    }

}