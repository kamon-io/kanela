package kanela.agent.util.classloader;

import lombok.val;

import java.net.URL;
import java.net.URLClassLoader;

public class ScalaCompilerClassLoaderFilter {

  /**
   * Tries to determine whether the ClassLoader causing errors belongs to the Scala Compiler on SBT. Since there is
   * no special naming or treatment of this particular ClassLoader it is impossible to filter it out from the
   * instrumentation process, but given that the jars found on it are quite particular (the compiler and jline) we can
   * assume that any instrumentation errors happening on that ClassLoader should be dismissed.
   *
   * We are doing this check here instead of using a ClassLoaderNameMatcher because this is a relatively expensive
   * check which might only be necessary in a few cases, so we rather filter the error than putting the burden of this
   * check on every single class load.
   */
  public static boolean isScalaCompilerClassLoader(ClassLoader classLoader) {
    if(classLoader instanceof URLClassLoader) {
      val urlClassLoader = (URLClassLoader) classLoader;
      boolean foundScalaCompiler = false;
      boolean foundJLine = false;

      for(URL url : urlClassLoader.getURLs()) {
        if(url.getFile().contains("scala-compiler"))
          foundScalaCompiler = true;
        if(url.getFile().contains("jline"))
          foundJLine = true;
      }

      return foundScalaCompiler && foundJLine;
    } else return false;
  }
}
