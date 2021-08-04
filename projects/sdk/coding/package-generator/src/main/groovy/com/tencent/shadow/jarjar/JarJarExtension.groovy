package com.tencent.shadow.jarjar

class JarJarExtension {
    String temp = 'build/temp'
    String into = 'build/libs'
    String libs = 'libs'
    String jarJarDependency = ''
    Map<String, List<String>> rules = [:]
    Map<String, List<String>> merge = [:]
}
