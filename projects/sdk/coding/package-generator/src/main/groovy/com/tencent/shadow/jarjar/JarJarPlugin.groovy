package com.tencent.shadow.jarjar


import com.tencent.shadow.jarjar.util.ZipUtils
import net.lingala.zip4j.core.ZipFile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.file.DuplicatesStrategy

import java.nio.channels.FileChannel

class JarJarPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create('jarJar', JarJarExtension)
        project.configurations {
            download
        }

        List<String> aarList = new ArrayList<>()
        project.task('jarJar', type: Copy) {
            group('jarjar')

            from(project.configurations.download)
            into(project.jarJar.temp)

            rename { String fileName ->
                if (fileName.toLowerCase().endsWith('.aar')) {
                    aarList.add(fileName)
                    fileName + ''
                } else {
                    fileName + '.original'
                }
            }

            exclude('jarjar*')
            duplicatesStrategy(DuplicatesStrategy.EXCLUDE)
        }.doLast {
            createJarjar(project)

            project.file("${project.jarJar.into}/").mkdirs()

            loadAar(project.file(project.jarJar.temp).toString(), aarList)

            def rules = project.jarJar.rules
            def destinationDir = project.file(project.jarJar.temp)

            rules.keySet().each { key ->
                if (new File(destinationDir, key).exists()) {
                    new File(destinationDir, key).deleteOnExit()
                }
                List<String> ruleParts = rules[key]
                wirteRuleFile(destinationDir, key + ".rule.txt", ruleParts)

                project.exec {
                    File file = project.file("${project.jarJar.into}/${key}")
                    if (file.exists() && file.isFile()) {
                        file.delete()
                    }

                    File ruleFile = new File(destinationDir.absolutePath, "${key}.rule.txt")
                    File srcJarFile = new File(destinationDir.absolutePath, "${key}.original")
                    File destJarFile = file
                    //println("> Jarjar :start change jar file ${file.name} package name")
                    commandLine('cmd', '/c', 'java', '-jar',
                            project.file(project.jarJar.jarJarDependency).absolutePath,
                            'process',
                            ruleFile.absolutePath,
                            srcJarFile.absolutePath,
                            destJarFile.absolutePath
                    )
                    println("> Jarjar :the jar file ${file.name} package name changed")
                }
            }

            File libsDir = project.file(project.jarJar.libs)
            String libs = project.jarJar.libs
            if (libs.toLowerCase().startsWith('${root}')){
                String path = libs.toLowerCase().replace('${root}','')
                libsDir = project.rootProject.file(path)
            }
            libsDir.mkdirs()

            Map<String, List<String>> merge = project.jarJar.merge
            if (merge == null || merge.size() == 0) {
                rules.keySet().each {
                    File src = new File(project.file(project.jarJar.into),it)
                    File dest = new File(libsDir,it)
                    copy(dest,src)
                }
            }else{
                merge.keySet().each { key ->
                    List<String> files = new ArrayList<>()
                    merge[key].forEach { name ->
                        files.add(project.file("${project.jarJar.into}/${name}").absolutePath)
                    }
                    File file = new File(libsDir,"${key}.jar")
                    if (file.exists() && file.isFile()){
                        file.delete()
                    }
                    ZipUtils.merge(file.absolutePath,files)
                    files.forEach{ f ->
                        boolean result = new File(f).delete()
                    }
                }
            }
        }.outputs.upToDateWhen { false }
    }

    void wirteRuleFile(File destinationDir, String fileName, List<String> rules) {
        File ruleFile = new File(destinationDir, fileName)
        if (ruleFile.exists() && ruleFile.isFile()) {
            ruleFile.delete()
        }

        ruleFile.withPrintWriter { out ->
            rules.each {
                out.print(it)
                out.print('\r\n')
            }
        }
    }

    private void createJarjar(Project project) {
        if (project.jarJar.jarJarDependency.length() <= 0) {
            project.jarJar.jarJarDependency = project.buildDir.absolutePath + File.separator + "jarJar/jarjar-1.7.2.jar"

            File dir = new File(project.buildDir.absolutePath + File.separator + "jarJar/")
            if (!dir.exists() || !dir.isDirectory()) {
                dir.mkdirs()
            }

            File file = new File(project.buildDir.absolutePath + File.separator + "jarJar/jarjar-1.7.2.jar")
            if (file.exists() && file.isFile()) {
                return
            }

            getClass().getResource('jarjar-1.7.2.jar').withInputStream { ris ->
                file.withOutputStream { fos ->
                    fos << ris
                }
            }
        }
    }

    private void loadAar(String dir, List<String> list) {
        list.forEach {
            File f = new File(dir, it)
            if (f.exists() && f.isFile()) {
                getJarFileFromAar(f)
            } else {
                throw new RuntimeException(f.name + "不存在")
            }
        }
    }

    private boolean getJarFileFromAar(File file) {
        try {
            ZipFile zipFile = new ZipFile(file)
            zipFile.extractFile("classes.jar", file.parentFile.absolutePath)
            File dest = new File(file.parentFile, "classes.jar")
            dest.renameTo(new File(file.parentFile, file.name.replace(".aar", ".jar.original")))
            file.delete()
            new File(file.parentFile, "classes.jar").delete()
        } catch (Throwable e) {
            e.printStackTrace()
        }
    }

    private void copy(File dest, File src) {
        FileChannel inputChannel = null
        FileChannel outputChannel = null
        inputChannel = new FileInputStream(src).getChannel()
        outputChannel = new FileOutputStream(dest).getChannel()
        outputChannel.transferFrom(inputChannel, 0, inputChannel.size())
        inputChannel.close()
        outputChannel.close()
    }
}
