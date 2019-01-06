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

package kanela.agent.api.instrumentation.listener;

import com.grack.nanojson.JsonStringWriter;
import com.grack.nanojson.JsonWriter;
import fi.iki.elonen.NanoHTTPD;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import kanela.agent.util.conf.KanelaConfiguration;
import lombok.val;

import java.io.IOException;
import java.util.Optional;

public class InstrumentationRegistryListenerServer {

    private Optional<EmbeddedHttpServer> embeddedHttpServer = Optional.empty();

    InstrumentationRegistryListenerServer() {
        try {
            startEmbeddedServer(KanelaConfiguration.instance());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String scrapeData() {
        val modules = JsonWriter.string().object().array("modules");
        val modulesJson = InstrumentationRegistryListener.instance().getRecorded().foldLeft(modules, (mods, m) -> addModuleJson(mods.object(), m)).end();
        return InstrumentationRegistryListener.instance().getErrors().foldLeft(modulesJson.array("errors"),
                (errs, e) -> e._2().foldLeft(errs.object().value("type", e._1).array(), (tws, tw) -> tws.value(tw.getMessage()))
                        .end()).end().end().done();
    }

    private JsonStringWriter addModuleJson(JsonStringWriter object, Tuple2<String, Map<String, List<String>>> module) {
        val instrumentations = object.value("name", module._1).array("instrumentations");
        return module._2.foldLeft(instrumentations, (inst, i) -> addInstrumentationJson(inst.object(), i)).end().end();
    }

    private JsonStringWriter addInstrumentationJson(JsonStringWriter object, Tuple2<String, List<String>> instrumentation) {
        val types = object.value("name", instrumentation._1).array("types");
        return instrumentation._2.foldLeft(types, (ts, t) -> ts.value(t)).end().end();
    }

    public void stop() {
        stopEmbeddedServer();
    }

    private class EmbeddedHttpServer extends NanoHTTPD {
        public EmbeddedHttpServer(String hostname, int port) {
            super(hostname, port);
        }

        public Response serve(IHTTPSession session) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", scrapeData());
        }
    }


    private void startEmbeddedServer(KanelaConfiguration config) throws IOException {
        val server = new EmbeddedHttpServer(
                config.getInstrumentationRegistryConfig().getHostname(),
                config.getInstrumentationRegistryConfig().getPort());

        server.start();
        embeddedHttpServer = Optional.of(server);
    }

    private void stopEmbeddedServer() {
        embeddedHttpServer.ifPresent(NanoHTTPD::stop);
    }
}

