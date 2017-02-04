import sbtbuildinfo._

case class JavaClassBuildInfoRender (options: Seq[BuildInfoOption], pkg: String, obj: String) extends BuildInfoRenderer {
  override def fileType = BuildInfoType.Source

  override def extension = "java"

  override def isSource = true

  override def renderKeys(infoKeysNameAndValues: Seq[BuildInfoResult]) =
    infoKeysNameAndValues.map { t => s"""  public static final ${t.typeExpr} ${t.identifier} = "${t.value}";""" } ++
    infoKeysNameAndValues.map { t => s"""  public static final ${t.typeExpr} ${t.identifier}() { return ${t.identifier}; }""" }
    

  def header: Seq[String] = Seq(
    s"package $pkg;",
    "",
    "public class BuildInfo {"
    )

  def footer: Seq[String] = Seq("}")
}
