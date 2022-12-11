package io.github.wycst.wast.common.template;

import io.github.wycst.wast.common.compiler.MemoryClassLoader;
import io.github.wycst.wast.common.compiler.MemoryJavaFileManager;
import io.github.wycst.wast.common.compiler.MemoryJavaFileObject;
import io.github.wycst.wast.common.exceptions.ParserException;
import io.github.wycst.wast.common.utils.RegexUtils;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串模板引擎
 * 参考jsp模板引擎
 * <p>
 * <% java代码；%>
 * <%=java表达式%>
 * ${k} el表达式解析
 *
 * <p> 当前仅仅内部orm进行代码自动生成模块使用，性能未做优化
 *
 * @Author: wangy
 * @Date: 2021/9/7 21:53
 * @Description:
 */
public final class StringTemplate {

    private static JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private static StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

    private static final AtomicLong ATOMIC_LONG = new AtomicLong(0);
    private static final String PACKAGE_NAME = StringTemplate.class.getPackage().getName();

    private static final String DEFINE_PREFIX = "$define ";
    private static final String FOR_LOOP_REGEX = "^[$]for[ ]*[(][ ]*(\\w+)(,[ ]*(\\w+))?[ ]*[)][ ]*in[ ]*(\\w+)$";
    private static final String IF_REGEX = "^[$]if[ ]*[(](.*?)[)]$";
    private static final String ELSE_IF_REGEX = "^[$]else[ ]+if[ ]*[(](.*?)[)]$";
    private static final String ELSE_REGEX = "^[$]else$";
    private static final String PLACEHOLDER_REGEX = ".*[^\\\\]?[$][{]([ ]*[0-9a-zA-Z_.$]+[ ]*)[}].*";

    private static final Pattern FOR_LOOP_PATTERN = Pattern.compile(FOR_LOOP_REGEX);
    private static final Pattern IF_PATTERN = Pattern.compile(IF_REGEX);
    private static final Pattern ELSE_IF_PATTERN = Pattern.compile(ELSE_IF_REGEX);
    private static final Pattern ELSE_PATTERN = Pattern.compile(ELSE_REGEX);

    private final String templateId = UUID.randomUUID().toString();
    private final byte[] templateJavaSource;
    private TemplateClass templateClass;
    // 静态字符串临时缓冲区（优化）
    private StringBuffer staticTempBuffer = new StringBuffer();
    private boolean nextNewLine = false;

    public StringTemplate(String template) {
        StringBuffer importPackages = new StringBuffer();
        importPackages.append("package " + PACKAGE_NAME + ";\r\n");
        importPackages.append("import java.util.*;\r\n");
//        importPackages.append("import io.github.wycst.wast.common.template.TemplateClass;\r\n");
//        importPackages.append("import io.github.wycst.wast.common.utils.*;\r\n");

        // 源代码
        StringBuffer source = new StringBuffer();
        String[] lines = template.split("\r\n", -1);

        boolean javaCodeBegin = false;
        boolean userLocalContext = false;
        boolean defineLocalContext = false;
        int scopeLevel = 0;
        int flag = 0;
        int index = 0, lineLength = lines.length;
        for (String line : lines) {
            index++;
            if (nextNewLine) {
                nextNewLine = false;
                // 如果以占位符号结尾，追加换行符
                appendIndent(source, scopeLevel);
                source.append("\t\tprintln();\r\n");
            } else {
                if (line.length() == 0) {
                    boolean hasAppendNewLine = staticTempBuffer.length() > 0;
                    this.persistBuffer(source);
                    // 如果最后一个line为空忽略掉（分割虚拟出的一行）
                    if (index < lineLength || !hasAppendNewLine) {
                        appendIndent(source, scopeLevel);
                        source.append("\t\tprintln();\r\n");
                    }
                    continue;
                }
            }
            // 转义符替换
            line = line.replace("\\", "\\\\");
            line = line.replace("\r", "\\r");
            line = line.replace("\n", "\\n");
            String trimLine = line.trim();
            if (trimLine.startsWith("<%")) {
                if (trimLine.endsWith("%>")) {
                    // java代码同一行
                    String lineCode = trimLine.substring(2, trimLine.length() - 2);
                    if (lineCode.startsWith("=")) {
                        // 解析表达式直接作为java代码执行
                        source.append("\t\tprintln(\"" + lineCode.substring(1).replace("\"", "\\\"") + "\");\r\n");
                    } else {
                        if (lineCode.startsWith("import ") && lineCode.endsWith(";")) {
                            importPackages.append(lineCode).append("\r\n");
                        } else {
                            // java代码
                            source.append("\t\t").append(lineCode).append("\r\n");
                        }
                    }
                } else {
                    // java代码开始
                    javaCodeBegin = true;
                }
            } else if (trimLine.endsWith("%>")) {
                // java代码结束
                javaCodeBegin = false;
            } else {
                if (javaCodeBegin) {
                    // import语句
                    if (trimLine.startsWith("import ") && trimLine.endsWith(";")) {
                        importPackages.append(line);
                    } else {
                        source.append("\t\t").append(line).append("\r\n");
                    }
                } else {
                    boolean isTextLine = true;
                    if (trimLine.startsWith("$")) {
                        isTextLine = false;
                        if (trimLine.matches(FOR_LOOP_REGEX)) {
                            persistBuffer(source);
                            if (!defineLocalContext) {
                                appendDefineSource(source, line, "Map<String,Object> localContext = null;");
                                appendDefineSource(source, line, "Iterable<Object> iterable = null;");
                                appendDefineSource(source, line, "int index = 0;");
                                defineLocalContext = true;
                            }
                            userLocalContext = appendForLoopSource(source, line, trimLine);
                            flag += 1;
                            scopeLevel++;
                        } else if (trimLine.startsWith(DEFINE_PREFIX)) {
                            this.persistBuffer(source);
                            appendIndent(source, scopeLevel);
                            appendDefineSource(source, line, trimLine);
                        } else if (trimLine.matches(IF_REGEX)) {
                            this.persistBuffer(source);
                            appendIndent(source, scopeLevel);
                            appendIfClauseSource(source, line, trimLine);
                            flag += 1;
                            scopeLevel++;
                        } else if (trimLine.matches(ELSE_IF_REGEX)) {
                            this.persistBuffer(source);
                            appendElseIfClauseSource(source, line, trimLine);
                            flag += 1;
                        } else if (trimLine.matches(ELSE_REGEX)) {
                            this.persistBuffer(source);
                            appendElseClauseSource(source, line, trimLine);
                            flag += 1;
                        } else if (trimLine.equals("$end")) {
                            this.persistBuffer(source);
                            if (scopeLevel > 0) {
                                scopeLevel--;
                            }
                            appendIndent(source, scopeLevel);
                            source.append("\t\t}\r\n");
                            flag -= 1;
                        } else {
                            // println
                            isTextLine = true;
                        }
                        if (flag == 0) {
                            userLocalContext = false;
                            if (scopeLevel > 0) {
                                scopeLevel--;
                            }
                        }
                    }
                    if (isTextLine) {
                        if (line.matches(PLACEHOLDER_REGEX)) {
                            this.persistBuffer(source);
                            line = line.replace("\"", "\\\"");
                            appendIndent(source, scopeLevel);
                            appendPlaceHolderLine(source, line, userLocalContext ? "localContext" : "context", scopeLevel);
//                            if (userLocalContext) {
//                                source.append("\t\tprintln(\"" + line + "\", localContext);\r\n");
//                            } else {
//                                source.append("\t\tprintln(\"" + line + "\", context);\r\n");
//                            }
                        } else {
                            // appendIndent(source, scopeLevel);
                            if (staticTempBuffer.length() == 0) {
                                appendIndent(source, scopeLevel);
                            }
                            // 追加静态缓冲区
                            this.appendBuffer(line.replace("\"", "\\\""));
//                             source.append("\t\tprintln(\"" + line.replace("\"", "\\\"") + "\");\r\n");
                        }
                    }
                }
            }
        }

        this.persistBuffer(source, false);

        String simpleClassName = genTemplateClassName();
        final String javaCode = getJavaCode(simpleClassName, importPackages, source);
        this.templateJavaSource = javaCode.getBytes();
        try {
            MemoryJavaFileManager javaFileManager = new MemoryJavaFileManager(fileManager);
            JavaFileObject javaFileObject = javaFileManager.createJavaFileObject(simpleClassName + ".java", javaCode);
            JavaCompiler.CompilationTask task = compiler.getTask(null, javaFileManager, null,
                    Arrays.asList(/*"-d", classPath, */"-encoding", "UTF-8"/*, "-XDuseUnsharedTable"*/), null,
                    Arrays.asList(javaFileObject));
            boolean bl = task.call();
            if (bl) {
                MemoryJavaFileObject memoryJavaFileObject = javaFileManager.getMemoryJavaFileObject();
                MemoryClassLoader memoryClassLoader = new MemoryClassLoader(memoryJavaFileObject);
                templateClass = (TemplateClass) memoryClassLoader.loadClass(PACKAGE_NAME + "." + simpleClassName).newInstance();
            }
        } catch (Throwable e) {
            throw new ParserException(" parse exception :" + e.getMessage(), e);
        }
    }

    private void appendIndent(StringBuffer source, int scopeLevel) {
        while (scopeLevel-- > 0) {
            source.append("\t");
        }
    }

    private String genTemplateClassName() {
        return String.format("StringTemplate$_%d", ATOMIC_LONG.getAndIncrement());
    }

    private String getJavaCode(String className, StringBuffer importPackages, StringBuffer source) {
        StringBuffer javaCodeBuffer = new StringBuffer();
        javaCodeBuffer.append(importPackages);
        javaCodeBuffer.append("public class ").append(className).append(" extends TemplateClass {\r\n");
        javaCodeBuffer.append("\r\n");
        javaCodeBuffer.append("\tprotected void renderTemplate(final Map<String,Object> context) {\r\n").append(source).append("\r\n").append("\t}\r\n");
        javaCodeBuffer.append("}");
        return javaCodeBuffer.toString();
    }

    private void appendDefineSource(StringBuffer source, String line, String trimLine) {
        this.persistBuffer(source);
        if (trimLine.startsWith(DEFINE_PREFIX)) {
            source.append("\t\t").append(trimLine.substring(DEFINE_PREFIX.length()));
        } else {
            source.append("\t\t").append(trimLine);
        }
        if (!trimLine.endsWith(";")) {
            source.append(";");
        }
        source.append("\r\n");
    }

    /**
     * 将line追加到缓冲区
     */
    private void appendBuffer(String line) {
        // println函数会自动追加换行, 临时静态字符串追加行文本（占一行）的最后一行不需要换行标识
        if (staticTempBuffer.length() > 0) {
            staticTempBuffer.append("\\r\\n");
        }
        staticTempBuffer.append(line);
    }

    /**
     * 解析上下文，模板核心代码
     */
    private void appendPlaceHolderLine(StringBuffer source, String line, String contextName, int scopeLevel) {
        // 暂时只支持解析表达式，可拓展支持函数调用，后续支持
        String reg = "[$][{]([ ]*[0-9a-zA-Z_.$]+[ ]*)[}]";
        Pattern pattern = RegexUtils.getPattern(reg);
        Matcher matcher = pattern.matcher(line);
        int beginIndex = 0;

        StringBuffer buffer = staticTempBuffer;
        boolean hasNewlineFlag = false;
        while (matcher.find()) {
            // 判断group开始是否为转义标识符\\,如果是就原group跳过
            int newBeginIndex = matcher.start(0);
            if (newBeginIndex > 1 && line.charAt(newBeginIndex - 1) == '\\') {
                if (!hasNewlineFlag) {
                    if (buffer.length() > 0) {
                        buffer.append("\\r\\n");
                        hasNewlineFlag = true;
                    }
                }
                buffer.append(line, beginIndex, newBeginIndex - 2);
                buffer.append(matcher.group(0));
            } else {
                String key = matcher.group(1).trim();
                buffer.append(line, beginIndex, newBeginIndex);
                // 这里需要动态append值,先持久化
                persistBuffer(source, false);
                appendIndent(source, scopeLevel);
                // 占位符实际替换代码
                source.append("\t\tprint(getContextValue(" + contextName + ", \"" + key + "\", \"\"));\r\n");
                appendIndent(source, scopeLevel);
            }
            beginIndex = matcher.end(0);
        }

        if (beginIndex < line.length()) {
//            appendIndent(source, scopeLevel);
            buffer.append(line, beginIndex, line.length());
        } else {
            nextNewLine = true;
        }
    }

    /**
     * 缓冲区持久化到source
     */
    private void persistBuffer(StringBuffer source) {
        persistBuffer(source, true);
    }

    /**
     * 缓冲区持久化到source
     */
    private void persistBuffer(StringBuffer source, int level) {
        this.appendIndent(source, level);
        persistBuffer(source, true);
    }

    /**
     * 缓冲区持久化到source
     */
    private void persistBuffer(StringBuffer source, boolean newLine) {
        if (staticTempBuffer.length() > 0) {
            if (newLine) {
                // 临时静态字符串持久化到source
                source.append("\t\tprintln(\"").append(staticTempBuffer).append("\");\r\n");
            } else {
                source.append("\t\tprint(\"").append(staticTempBuffer).append("\");\r\n");
            }
            // 重置
            staticTempBuffer.setLength(0);
        }
    }

    private boolean appendForLoopSource(StringBuffer source, String line, String trimLine) {
        Matcher matcher = FOR_LOOP_PATTERN.matcher(trimLine);
        boolean matched = false;
        if (matcher.find()) {
            matched = true;
            String itemKey = matcher.group(1);
            String indexKey = matcher.group(3);
            String itemsKey = matcher.group(4);
            source.append("\t\titerable = getContextIterable(context, \"" + itemsKey + "\");\r\n");
            source.append("\t\tlocalContext = new HashMap<String,Object>(context);\r\n");
            if (indexKey != null) {
                source.append("\t\tindex = 0;\r\n");
            }
            source.append("\t\tfor (Object " + itemKey + " : iterable) {\r\n");
            source.append("\t\t\tlocalContext.put(\"" + itemKey + "\", " + itemKey + ");\r\n");
            if (indexKey != null) {
                source.append("\t\t\tlocalContext.put(\"" + indexKey + "\", index++);\r\n");
            }
        } else {
            // 理论上代码不可达，暂不处理
        }
        return matched;
    }

    private boolean appendIfClauseSource(StringBuffer source, String line, String trimLine) {
        Matcher matcher = IF_PATTERN.matcher(trimLine);
        boolean matched = false;
        if (matcher.find()) {
            matched = true;
            String condition = matcher.group(1);
            source.append("\t\tif(").append(condition).append("){\r\n");
        } else {
            // 理论上代码不可达，暂不处理
        }
        return matched;
    }

    private boolean appendElseIfClauseSource(StringBuffer source, String line, String trimLine) {
        Matcher matcher = ELSE_IF_PATTERN.matcher(trimLine);
        boolean matched = false;
        if (matcher.find()) {
            matched = true;
            String condition = matcher.group(1);
            source.append("\t\t} else if(").append(condition).append("){\r\n\t");
        } else {
            // 理论上代码不可达，暂不处理
        }
        return matched;
    }

    private void appendElseClauseSource(StringBuffer source, String line, String trimLine) {
        source.append("\t\t} else {\r\n\t");
    }

    private Map<String, Object> context = new HashMap<String, Object>();

    public void binding(String key, Object value) {
        context.put(key, value);
    }

    public void binding(Map<String, Object> data) {
        context.putAll(data);
    }

    public void clearBinging() {
        context.clear();
    }

    public void clearBinging(String key) {
        context.remove(key);
    }

    /***
     * 生成模板串
     *
     * @return
     */
    public synchronized String render() {
        return render(context);
    }

    /**
     * 以指定上下文渲染数据模板
     *
     * @param context
     * @return
     */
    public synchronized String render(Map<String, Object> context) {
        return templateClass.render(context);
    }

    /***
     * 返回实例id
     *
     * @return
     */
    public final String getId() {
        return templateId;
    }

    /**
     * 获取模板源代码
     */
    byte[] getTemplateJavaSource() {
        return templateJavaSource;
    }
}
