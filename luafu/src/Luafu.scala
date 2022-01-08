import org.objectweb.asm._
import org.objectweb.asm.tree.{ClassNode, MethodNode}

import java.lang.reflect.{Method, Modifier}
import java.nio.file.{Files, Paths}
import java.util.zip.{ZipEntry, ZipFile}
import scala.collection.mutable.ListBuffer
import scala.io.{Source, StdIn}
import scala.jdk.CollectionConverters._
import scala.util.control.Exception.{allCatch, ultimately}

object Luafu:
  def main(args: Array[String]): Unit =
    val jar         = Jar(ZipFile("lib\\wakfu-client.jar"))
    val descriptors = ScriptDescriptors.resolve(jar).get
    val context     = ScriptContext.load(jar, descriptors).get

    getClass.getClassLoader
      .loadClass("com.ankamagames.wakfu.client.WakfuClient")
      .getMethod("main", classOf[Array[String]])
      .invoke(null, Array[String]())

    while (true) {
      val line = StdIn.readLine
      allCatch.withApply(println)(context.interpret(line))
    }

case class ScriptDescriptors(
    scriptManagerClass: String,
    loadScriptDescriptor: String
):
  val scriptLibraryClass: String =
    Type.getArgumentTypes(loadScriptDescriptor)(1).getElementType.getClassName

  val scriptClass: String =
    Type.getReturnType(loadScriptDescriptor).getClassName

object ScriptDescriptors:
  def resolve(jar: Jar): Option[ScriptDescriptors] =
    for
      scriptManagerClass <- jar.classes
        .map { clazz =>
          val finder = FieldConstantFinder(str => str == ".lua" || str == "script")
          clazz.accept(finder, Opcodes.ASM9)

          if (finder.values.length == 2)
            val node = ClassNode()
            clazz.accept(node, Opcodes.ASM9)
            Some(node)
          else None
        }
        .collectFirst { case Some(res) => res }

      loadScriptMethod <-
        scriptManagerClass.methods.asScala.find { m =>
          val args = Type.getArgumentTypes(m.desc)
          val ret  = Type.getReturnType(m.desc)

          (m.access & Opcodes.ACC_PRIVATE) != 0 &&
          (args.length == 3) &&
          ret.getSort == Type.OBJECT &&
          args(0).getSort == Type.OBJECT &&
          args(1).getSort == Type.ARRAY &&
          args(2) == Type.BOOLEAN_TYPE
        }
    yield ScriptDescriptors(scriptManagerClass.name, loadScriptMethod.desc)

class ScriptContext(
    scriptManager: AnyRef,
    scriptLibraries: AnyRef,
    loadScript: Method,
    runScript: Method
):
  loadScript.setAccessible(true)
  private val pattern = """dofile\(['"](.*)['"]\)""".r

  def interpret(code: String): Unit =
    val source = pattern.findFirstMatchIn(code) match
      case Some(res) =>
        val file = res.group(1)
        val path = Paths.get("luafu").resolve("scripts").resolve(file)
        new String(Files.readAllBytes(path))
      case None => code

    val script = loadScript.invoke(scriptManager, source, scriptLibraries, false)
    runScript.invoke(script, java.util.HashMap())

object ScriptContext:
  def load(jar: Jar, descriptors: ScriptDescriptors): Option[ScriptContext] =
    val managerClass = getClass.getClassLoader.loadClass(descriptors.scriptManagerClass)
    val scriptClass  = getClass.getClassLoader.loadClass(descriptors.scriptClass)

    for
      scriptManager <- managerClass.getDeclaredMethods
        .find(m => Modifier.isStatic(m.getModifiers) && m.getParameterCount == 0)
        .map(_.invoke(null))
      scriptLibraries = loadAllLibraries(jar, descriptors)

      loadScript <- managerClass.getDeclaredMethods
        .find(m => Type.getMethodDescriptor(m) == descriptors.loadScriptDescriptor)
      runScript <- scriptClass.getDeclaredMethods
        .find(m => m.getParameterTypes.sameElements(Array(classOf[java.util.Map[_, _]])))
    yield ScriptContext(scriptManager, scriptLibraries, loadScript, runScript)

  def loadAllLibraries(jar: Jar, descriptors: ScriptDescriptors): AnyRef =
    val libraryBaseClass = getClass.getClassLoader.loadClass(descriptors.scriptLibraryClass)
    val libraryClasses   = jar.classes.filter(_.getSuperName == descriptors.scriptLibraryClass)
    val array = java.lang.reflect.Array.newInstance(libraryBaseClass, libraryClasses.length)

    for ((libClassReader, i) <- libraryClasses.zipWithIndex)
      getClass.getClassLoader
        .loadClass(libClassReader.getClassName)
        .getDeclaredMethods
        .find(m => Modifier.isStatic(m.getModifiers) && m.getParameterCount == 0)
        .foreach(m => java.lang.reflect.Array.set(array, i, m.invoke(null)))

    array

class Jar(zip: ZipFile):
  def classes: Iterator[ClassReader] =
    zip.entries.asScala
      .filter((entry: ZipEntry) => entry.getName.endsWith(".class"))
      .map { (entry: ZipEntry) =>
        val is = zip.getInputStream(entry)
        ultimately(is.close())(ClassReader(is))
      }

class FieldConstantFinder(fieldPred: AnyRef => Boolean) extends ClassVisitor(Opcodes.ASM9):
  val values = ListBuffer.empty[AnyRef]

  override def visitField(
      access: Int,
      name: String,
      descriptor: String,
      signature: String,
      value: AnyRef
  ): FieldVisitor =
    if (fieldPred(value))
      this.values += value
    super.visitField(access, name, descriptor, signature, value)
