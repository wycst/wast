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

    private static final JavaCompiler JAVA_COMPILER = ToolProvider.getSystemJavaCompiler();
    private static final StandardJavaFileManager FILE_MANAGER = JAVA_COMPILER.getStandardFileManager(null, null, null);

    public synchronized static Class<?> compileJavaSource(JavaSourceObject sourceObject) {
        return compileJavaSource(sourceObject, ClassLoader.getSystemClassLoader());
    }

    public synchronized static Class<?> compileJavaSource(JavaSourceObject sourceObject, ClassLoader classLoader) {
        MemoryJavaFileManager javaFileManager = new MemoryJavaFileManager(FILE_MANAGER);
        try {
            List<String> options = null;
            if (EnvUtils.JDK_16_PLUS) {
                options = Arrays.asList("-encoding", "UTF-8", "-XDuseUnsharedTable", "-Xlint:-options");
            } else {
                options = Arrays.asList("-encoding", "UTF-8", "-XDuseUnsharedTable");
            }
            JavaFileObject javaFileObject = javaFileManager.createJavaFileObject(sourceObject.className + ".java", sourceObject.javaSourceCode);
            JavaCompiler.CompilationTask task = JAVA_COMPILER.getTask(null, javaFileManager, null,
                    options, null,
                    Arrays.asList(javaFileObject));
            boolean bl = task.call();
            if (bl) {
                MemoryJavaFileObject memoryJavaFileObject = javaFileManager.getLastMemoryJavaFileObject();
                MemoryClassLoader memoryClassLoader = new MemoryClassLoader(memoryJavaFileObject, classLoader);
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
        return compileJavaSources(ClassLoader.getSystemClassLoader(), sourceObjects);
    }

    public synchronized static List<Class<?>> compileJavaSources(ClassLoader classLoader, JavaSourceObject... sourceObjects) {
        MemoryJavaFileManager javaFileManager = new MemoryJavaFileManager(FILE_MANAGER, sourceObjects);
        try {
            List<Class<?>> targetList = new ArrayList<Class<?>>();
            List<JavaFileObject> javaFileObjects = new ArrayList<JavaFileObject>();
            for (JavaSourceObject javaSourceObject : sourceObjects) {
                javaFileObjects.add(javaFileManager.createJavaFileObject(javaSourceObject.className + ".java", javaSourceObject.javaSourceCode));
            }
            JavaCompiler.CompilationTask task = JAVA_COMPILER.getTask(null, javaFileManager, null,
                    Arrays.asList(/*"-d", classPath, */"-encoding", "UTF-8", "-XDuseUnsharedTable"), null,
                    javaFileObjects);
            boolean bl = task.call();
            if (bl) {
                Map<String, MemoryJavaFileObject> memoryJavaFileObject = javaFileManager.getFileObjectMap();
                MemoryClassLoader memoryClassLoader = new MemoryClassLoader(classLoader);
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
