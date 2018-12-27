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

import fi.iki.elonen.NanoHTTPD;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import kanela.agent.api.instrumentation.TypeTransformation;
import kanela.agent.util.classloader.ClassLoaderNameMatcher;
import kanela.agent.util.conf.KanelaConfiguration;
import lombok.val;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.io.IOException;
import java.util.Optional;

public class InstrumentationRegistryListener extends AgentBuilder.Listener.Adapter {

    private static InstrumentationRegistryListener _instance;
    private Optional<EmbeddedHttpServer> embeddedHttpServer = Optional.empty();

    private Map<String, List<Tuple2<TypeTransformation, List<TypeDescription>>>> moduleTransformers = HashMap.empty();
    private Map<String, List<Throwable>> errors = HashMap.empty();

    InstrumentationRegistryListener() {
        try {
            startEmbeddedServer(KanelaConfiguration.instance());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Map<String, List<String>>> getRecorded() {
        return moduleTransformers.mapValues(value -> value.toMap((t) -> Tuple.of(t._1.getInstrumentationName(), t._2.map(TypeDescription::getCanonicalName))));
    }

    public Map<String, List<Throwable>> getErrors() {
        return errors;
    }

    public void register(String moduleName, TypeTransformation typeTransformation) {
        moduleTransformers = moduleTransformers.computeIfPresent(moduleName, (mn, tts) -> tts.append(Tuple.of(typeTransformation, List.empty())))._2;
        moduleTransformers = moduleTransformers.computeIfAbsent(moduleName, (m) -> List.of(Tuple.of(typeTransformation, List.empty())))._2;
    }

    // needed for a single instance between classloaders
    private static Class getClass(String classname)
            throws ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if(classLoader == null)
            classLoader = InstrumentationRegistryListener.class.getClassLoader();
        return (classLoader.loadClass(classname));
    }

    public static InstrumentationRegistryListener instance() {
        if(_instance == null) {
            try {
                _instance = (InstrumentationRegistryListener) getClass("kanela.agent.api.instrumentation.listener.InstrumentationRegistryListener").newInstance();
            } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return _instance;
    }

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
        moduleTransformers = moduleTransformers.map(
                (moduleName, transformations) -> Tuple.of(moduleName, transformations.map(transformation -> {
                    if (transformation._1.getElementMatcher().map(em -> em.matches(typeDescription)).getOrElse(false) &&
                        ClassLoaderNameMatcher.RefinedClassLoaderMatcher.from(transformation._1.getClassLoaderRefiner()).matches(classLoader)
                    ) {
                        return transformation.update2(transformation._2.append(typeDescription));
                    } else {
                        return transformation;
                    }
                })));
    }

    @Override
    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
        errors = errors.computeIfPresent(typeName, (tn, errs) -> errs.append(throwable))._2;
        errors = errors.computeIfAbsent(typeName, (tn) -> List.of(throwable))._2;
    }

    public String scrapeData() {
        StringBuilder sb = new StringBuilder();
        this.getRecorded().forEach(
                (s, m) -> {
                    sb.append("*─ ");
                    sb.append(s);
                    sb.append("\n");
                    m.forEach(
                            (ss, mm) -> {
                                sb.append(" ├─ ");
                                sb.append(ss);
                                sb.append("\n");
                                mm.forEach(i -> {
                                    sb.append(" │└─ ");
                                    sb.append(i);
                                    sb.append("\n");
                                });
                            });
                });
        return sb.toString();
    }

    public void stop() {
        stopEmbeddedServer();
    }

    private class EmbeddedHttpServer extends NanoHTTPD {
        public EmbeddedHttpServer(String hostname, int port) {
            super(hostname, port);
        }

        public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
            return newFixedLengthResponse(Response.Status.OK, "text/plain; version=0.0.4; charset=utf-8", scrapeData());
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

