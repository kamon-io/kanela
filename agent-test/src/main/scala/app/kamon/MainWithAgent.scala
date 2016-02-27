package app.kamon

import java.io.{File, FileOutputStream}
import java.net.URL
import java.util
import java.util.Collections

import app.kamon.instrumentation.Pepe
import kamon.agent.utils.PluginCacheUtils
import org.slf4j.LoggerFactory

object MainWithAgent {

  def logger = LoggerFactory.getLogger(MainWithAgent.getClass)

  def main(args: Array[String]) {
    logger.info("Start Run Agent Test")
    new Pepe().hello()
    logger.info("Exit Run Agent Test")
  }

}

object MergeLog4jFuckingDat extends App{
  import scala.collection.JavaConverters._
  val tempFiles = new util.ArrayList[File]()
  val pluginCache = new PluginCacheUtils()

  tempFiles.add(new File("/home/diego/gitHub/kamon-agent/agent/target/scala-2.11/classes/META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat"))
  tempFiles.add(new File("/home/diego/Log4j2Plugins.dat"))

//  tempFiles.add(File.createTempFile("Log4j2Plugins2", "dat"))

  pluginCache.loadCacheFiles(getUrls())
  pluginCache.writeCache(new FileOutputStream(new File("/home/diego/Log4j2PluginsPuto2.dat")))

  def  getUrls(): util.Enumeration[URL] ={
    val urls = new util.ArrayList[URL]()
    for (tempFile:File <- tempFiles.asScala) {
      val url = tempFile.toURI.toURL
      urls.add(url)
    }
    Collections.enumeration(urls)
  }
}