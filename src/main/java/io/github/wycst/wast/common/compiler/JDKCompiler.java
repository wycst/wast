package io.github.wycst.wast.common.compiler;

import io.github.wycst.wast.common.utils.EnvUtils;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.util.*;

/**
 * @Date 2024/3/16 22:17
 * @Created by wangyc
 */
public class JDKCompiler {

    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private static final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

    public static Class<?> compileJavaSource(JavaSourceObject sourceObject) {
        MemoryJavaFileManager javaFileManager = new MemoryJavaFileManager(fileManager);
        try {
            List<String> options = null;
            if(EnvUtils.JDK_16_PLUS) {
                options = Arrays.asList("-encoding", "UTF-8", "-XDuseUnsharedTable", "-Xlint:-options");
            } else {
                options = Arrays.asList("-encoding", "UTF-8", "-XDuseUnsharedTable");
            }
            JavaFileObject javaFileObject = javaFileManager.createJavaFileObject(sourceObject.className + ".java", sourceObject.javaSourceCode);
            JavaCompiler.CompilationTask task = compiler.getTask(null, javaFileManager, null,
                    options, null,
                    Arrays.asList(javaFileObject));
            boolean bl = task.call();
            if (bl) {
                MemoryJavaFileObject memoryJavaFileObject = javaFileManager.getLastMemoryJavaFileObject();
                MemoryClassLoader memoryClassLoader = new MemoryClassLoader(memoryJavaFileObject);
                return memoryClassLoader.loadClass(sourceObject.packageName + "." + sourceObject.className);
            } else {
                throw new Exception("ERROR");
            }
        } catch (Throwable e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("compile exception :" + e.getMessage(), e);
        } finally {
            try {
                javaFileManager.close();
            } catch (IOException e) {
            }
        }
    }

    public static List<Class<?>> compileJavaSources(JavaSourceObject... sourceObjects) {
        MemoryJavaFileManager javaFileManager = new MemoryJavaFileManager(fileManager, sourceObjects);
        try {
            List<Class<?>> targetList = new ArrayList<Class<?>>();
            List<JavaFileObject> javaFileObjects = new ArrayList<JavaFileObject>();
            for (JavaSourceObject javaSourceObject : sourceObjects) {
                javaFileObjects.add(javaFileManager.createJavaFileObject(javaSourceObject.className + ".java", javaSourceObject.javaSourceCode));
            }
            JavaCompiler.CompilationTask task = compiler.getTask(null, javaFileManager, null,
                    Arrays.asList(/*"-d", classPath, */"-encoding", "UTF-8", "-XDuseUnsharedTable"), null,
                    javaFileObjects);
            boolean bl = task.call();
            if (bl) {
                Map<String, MemoryJavaFileObject> memoryJavaFileObject = javaFileManager.getFileObjectMap();
                MemoryClassLoader memoryClassLoader = new MemoryClassLoader();
                Set<Map.Entry<String, MemoryJavaFileObject>> entrySet = memoryJavaFileObject.entrySet();
                for (Map.Entry<String, MemoryJavaFileObject> entry : entrySet) {
                    targetList.add(memoryClassLoader.loadClass(entry.getKey(), entry.getValue().getBytes()));
                }
                return targetList;
            } else {
                throw new Exception("ERROR");
            }
        } catch (Throwable e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("compile java fail :" + e.getMessage(), e);
        } finally {
            try {
                javaFileManager.close();
            } catch (IOException e) {
            }
        }
    }


}
