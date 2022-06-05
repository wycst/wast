# wast

## 简介

一个轻量级java库，包含json, yaml, common, clients,其他模块视情况陆续放出。

## json模块

> 1 java语言整体性能最快的json库之一；<br>
> 2 功能全面，支持IO流文件读写，JSON节点树按需解析， 局部解析，序列化格式化，驼峰下划线自动转换；<br>
> 3 源码实现简单易懂，阅读调试都很容易；<br>
> 4 基本上无缓存，内存占用少，不会内存溢出；<br>
> 5 代码轻量，使用安全，没有漏洞风险；<br>
> 6 兼容jdk1.6+；

关于本json库与主流json库性能pk(原库名字叫light) <br>
[https://blog.csdn.net/wangych0112/article/details/122522193](https://blog.csdn.net/wangych0112/article/details/122522193)

## yaml模块

> 1 目前java语言解析yaml最快的库，性能大概是snakeyaml的5-20倍；<br>
> 2 支持文件流，URL, 字符数组，字符串等解析；<br>
> 3 支持常用yaml语法以及类型转换；<br>
> 4 内置Yaml节点模型；<br>
> 5 代码轻量，使用安全，没有漏洞风险；<br>
> 6 兼容jdk1.6+；

后续增加Yaml节点查找能力。

## 表达式引擎

> 1 java表达式引擎，以解析性能来说比现有开源的其他引擎都快，包括mvel, spel, fel等；<br>
> 2 支持java中所有的操作运算符（加减乘除余，位运算，逻辑运算，字符串+）；<br>
> 3 支持**指数运算(java本身不支持)； <br>
> 4 支持函数以及自定义函数实现,函数可以任意嵌套； <br>
> 5 科学记数法支持，16进制，8进制等解析，支持大数运算（大数统一转为double类型）；<br>
> 6 支持简单的三目运算；<br>
> 7 代码轻量，使用安全，没有漏洞风险；<br>
> 8 兼容jdk1.6+；

## 如何使用JSON模块

### 1 常用对象json序列化

```
Map map = new HashMap();
map.put("msg", "hello, light json !");
String result = JSON.toJsonString(map);
```

### 2 对象序列化到文件

```
Map map = new HashMap();
map.put("msg", "hello, light json !");
JSON.writeJsonTo(map, new File("/tmp/test.json"));
```

### 3 格式序列化

```
Map map = new HashMap();
map.put("name", "zhangsan");
map.put("msg", "hello, light json !");
JSON.toJsonString(map, WriteOption.FormatOut);

输出结果：
{
	"msg":"hello, light json !",
	"name":"zhangsan"
}
```

### 4 反序列化

```
String json = "{\"msg\":\"hello, light json !\",\"name\":\"zhangsan\"}";
Map map = (Map) JSON.parse(json);
System.out.println(map);
// 输出
{msg=hello, light json !, name=zhangsan}
```

### 5 指定类型反序列化

```
    String json = "{\"msg\":\"hello, light json !\",\"name\":\"zhangsan\"}";
    Map map = JSON.parseObject(json, Map.class);
    System.out.println(map);
    // 输出
    {msg=hello, light json !, name=zhangsan}
```

### 6 基于输入流的读取解析

```
    Map result = null;
    
    // 1 读取网络资源 GET
    result = JSON.read(new URL("https://developer.aliyun.com/artifact/aliyunMaven/searchArtifactByGav?groupId=spring&artifactId=&version=&repoId=all&_input_charset=utf-8"), Map.class);
    
    // 2 读取输入流
    InputStream inputStream = InputStreamTest.class.getResourceAsStream("/sample.json");
    result = JSON.read(inputStream, Map.class);
    
    // 3 读取文件
    result = JSON.read(new File("/tmp/smaple.json"), Map.class);
```

### 7 基于输入流的按需解析

提供JSONReader类可按需解析一个输入流，自定义解析，可随时终止。

```
        final JSONReader reader = JSONReader.from(new File(f));
        reader.read(new JSONReader.StreamReaderCallback(JSONReader.ReadParseMode.ExternalImpl) {
            @Override
            public void parseValue(String key, Object value, Object host, int elementIndex, String path) throws Exception {
                super.parseValue(key, value, host, elementIndex, path);
                if(path.equals("/features/[100000]/properties/STREET")) {
                     // 终止读取;
                    abort();
                }
            }
        }, true);
```

### 8 强大的JSONNode功能

> 1、支持对大文本json的懒加载解析功能，即访问时解析，当需要读取一个大文本json中一个或多个属性值时非常有用。<br>
> 2、支持按需解析；比如常规模式下完整解析需要耗时7-8秒左右，使用JSONNode可以在几百毫秒左右完成。<br>
> 3、支持上下文查找；<br>
> 4、支持在大文本json中提取部分内容作为解析结果（性能最高）,使用JSONNode.from(source, path, lazy?)  <br>
> 5、支持对节点的属性修改，删除等，节点的JSON序列化回显；<br>
> 6、支持直接提取功能(v0.0.2+支持)

使用'/'作为路径的分隔符，数组下标使用[n]访问支持[*], [n+], [n-],[n]等复合下标访问

```
  String json = "{\"name\": \"li lei\", \”properties\": {\"age\": 23}}";
  JSONNode node = JSONNode.parse(json);
  // 获取当前节点（根节点）的name属性
  String name = node.getChildValue("name", String.class);
  
  // 通过getPathValue方法可以获取任意路径上的值
  int age = node.getPathValue("/properties/age", int.class);
  
  // 通过path可以定位到任何路径节点
  JSONNode anyNode = JSONNode.get(path);
  
  // 通过root方法可以在任何节点回到根节点
  JSONNode root = anyNode.root();
  // root == node true
  
  // 根据路径局部解析
  JSONNode propertiesRoot = JSONNode.from(json, "/properties");
  // 局部解析懒加载(一般在获取某个数组的长度或者对象的keys等特别适用)
  JSONNode propertiesRoot = JSONNode.from(json, "/properties", true);
  propertiesRoot.keyNames();
  
  
  String json2 = `{
                      "store": {
                        "book": [
                          {
                            "category": "reference",
                            "author": "Nigel Rees",
                            "title": "Sayings of the Century",
                            "attr": {
                              "pos": "p1"
                            },
                            "price": 8.95
                          },
                          {
                            "category": "fiction",
                            "author": "Evelyn Waugh",
                            "title": "Sword of Honour",
                            "attr": {
                              "pos": "p2"
                            },
                            "price": 12.99
                          },
                          {
                            "category": "fiction",
                            "author": "Herman Melville",
                            "title": "Moby Dick",
                            "isbn": "0-553-21311-3",
                            "attr": {
                              "pos": "p3"
                            },
                            "price": 8.99
                          },
                          {
                            "category": "fiction",
                            "author": "J. R. R. Tolkien",
                            "title": "The Lord of the Rings",
                            "isbn": "0-395-19395-8",
                            "attr": {
                              "pos": "p4"
                            },
                            "price": 22.99
                          }
                        ],
                        "bicycle": {
                          "color": "red",
                          "price": 19.95
                        }
                      },
                      "expensive": 10
                    }`
;
  
  // 直接提取所有的author使用[*]
  List authors = JSONNode.extract(json2, "/store/book/[*]/author");
  
  // 提取第2本书的作者author使用指定的下标[n]
  String author = JSONNode.extract(json2, "/store/book/[1]/author").get(1);
  （或者 JSONNode.from(json2, "/store/book/[1]/author").getStringValue();性能大体一致)
  
  // 提取前2本书的作者使用下标[n-]（包含n）
  List authors = JSONNode.extract(json2, "/store/book/[1-]/author").get(1);
  
  // 提取从第2本书开始后面所有的作者使用下标[n+]（包含n）
  List authors = JSONNode.extract(json2, "/store/book/[1+]/author");
  
  
```

### 9 SpringBoot(Spring MVC) 集成

```
  @Configuration
  public class AppConfiguration implements WebMvcConfigurer {
 
       @Bean
       public HttpMessageConverters jsonHttpMessageConverters() {
           JSONHttpMessageConverter jsonHttpMessageConverter = new JSONHttpMessageConverter();
           jsonHttpMessageConverter.setWriteOptions(WriteOption... writeOptions);
           jsonHttpMessageConverter.setReadOptions(ReadOption... readOptions);
           return new HttpMessageConverters(jsonHttpMessageConverter);
       }
 
  }
```

### 10 序列化和反序列化支持配置

序列化配置枚举类：WriteOption

| 枚举值  | 说明  |
| ------------ | ------------ |
| FormatOut   | 格式化缩进输出  |
| FullProperty   | 输出完整的属性字段  |
| WriteDateAsTime   | 默认将日期格式化输出，配置此项可以序列化为时间戳 |
| SkipCircularReference  | 开始后探测序列化是否会存在循环引用造成的死循环  |
| BytesArrayToHex   | 默认情况下byte数组会输出为base64字符串，开启配置后将bytes数组输出为16进制字符串   |
| BytesArrayToNative   | 默认情况下byte数组会输出为base64字符串，开启配置后将bytes数组输出原生字节数组   |
| SkipGetterOfNoneField   | 是否跳过不存在属性域的getter方法序列化   |
| KeepOpenStream   | 序列化后不关闭流，默认自动关闭流，开启后不会调用close   |
| AllowUnquotedMapKey   | 默认情况下map的key统一加双引号输出,开启后将根据实际的key值类型序列化   |
| UseFields   | 默认通过实体类的getter方法序列化，开启后使用field字段进行序列化   |

反序列化配置枚举类：ReadOption

| 枚举值  | 说明  |
| ------------ | ------------ |
| ByteArrayFromHexString   | 目标类型为byte[]，解析到字符串标记时将按16进制字符串转化为byte[]数组(2个字符转化为一个字节)  |
| AllowComment   | 非标准json特性：允许JSON存在注释，仅仅支持//和/+* *+/，默认关闭注释解析  |
| AllowUnquotedFieldNames  | 非标准json特性：允许JSON字段的key没有双引号  |
| AllowSingleQuotes   | 非标准json特性：允许JSON字段的key使用单引号，注意仅仅是key   |
| UnknownEnumAsNull   | 不存在的枚举类型解析时默认抛出异常，开启后解析为null   |
| UseDefaultFieldInstance   | 解析实体bean的场景下，如果其属性的类型为普通抽象类或者接口(Map和Collection极其子类接口除外)，如果指定了默认实例将使用默认实例对象,从使用上解决类型映射问题，而不用趟AutoType带来的各种安全漏洞的坑   |
| UseBigDecimalAsDefaultNumber   | 开启后在不确定number类型情况下，统一转化为BigDecimal；默认自动判断number类型转化为int或long或者double   |

## yaml使用

```
// 解析返回文档结构
YamlDocument doc = YamlDocument.parse(yamlStr);
Properties props = doc.toProperties();
Map map = doc.toMap();
T t = doc.toEntity(T.class);

// 直接解析返回类型
T t = YamlDocument.parse(yamlStr, T.class);
```

## 表达式引擎使用

(支持编译模式，即将表达式生成字节码然后运行，虽然最快但需要缓存，特俗情况下可使用,不推荐，因为解析模式下已经快到极限了。)

### 1 直接运行模式

```
Expression.eval("1+1");  // 输出2
Expression.eval("1+1+'a'");  // 输出2a

Map map = new HashMap();
map.put("a", 1);
map.put("b", 2);
map.put("c", 3);
Expression.eval("a+b+c",map);  // 输出6
```

### 2 解析模式

将表达式解析为模型，一般使用在动态表达式场景

```
Map map = new HashMap();
map.put("a", 1);
map.put("b", 2);
map.put("c", 3);
Expression varExpr = Expression.parse("a + b + c");  // 只需要解析一次
varExpr.evaluate(map);     // 输出6

map.put("c", 30);
varExpr.evaluate(map);     // 输出33
```

### 3 函数和自定义函数使用

参考light-test ExprFunctionTest类<br>
内置函数： max/min/sum/avg/abs/sqrt/lower/upper/size/ifNull <br>
源码见： BuiltInFunction <br>
自定义函数可以是全局函数不需要类名作为命名空间，使用@max直接调用，全局函数可以通过两种方式注册：<br>

```
 // 方式1
 evaluateEnvironment.registerStaticMethods(true, Math.class, String.class);
 // 方式2
 evaluateEnvironment.registerFunction("min", new ExprFunction<Object, Number>() {
        @Override
        public Number call(Object... params) {
        Arrays.sort(params);
        return (Number) params[params.length - 1];
        }
});
```

也可以是命名空间函数，使用时需要添加类名简称（命名空间）如@Math.max(a,b)
函数使用@标记+函数名称

```
        Map context = new HashMap();
        context.put("tip", "1 ");
        context.put("name", "zhangsan, %s");
        context.put("msg", "hello");
        context.put("type", 1);
        context.put("a", 1);
        context.put("b", 12);
        context.put("c", 111);
        context.put("B6_AvgCpuUsed", 1.0);
        context.put("B5_AvgCpuUsed", 2.0);
        context.put("B4_AvgCpuUsed", 3.0);
        context.put("vars", new String[] {"hello"});
        
        EvaluateEnvironment evaluateEnvironment = EvaluateEnvironment.create(context);
        evaluateEnvironment.registerStaticMethods(Math.class, String.class);

//        evaluateEnvironment.registerFunction("MAX", new ExprFunction<Object, Number>() {
//            @Override
//            public Number call(Object... params) {
//                Arrays.sort(params);
//                return (Number) params[params.length - 1];
//            }
//        });
        evaluateEnvironment.registerFunction("min", new ExprFunction<Object, Number>() {
            @Override
            public Number call(Object... params) {
                Arrays.sort(params);
                return (Number) params[params.length - 1];
            }
        });

//        Object result = Expression.eval("@Math.max(B6_AvgCpuUsed,(B5_AvgCpuUsed+B6_AvgCpuUsed))/2.0 + @Math.max(B6_AvgCpuUsed,B5_AvgCpuUsed)", evaluateEnvironment);

        System.out.println( Expression.eval("@min(@sum(a,b,c), 50, 125, 2, -11)", evaluateEnvironment));
        System.out.println( Expression.eval("@max(@sum(a,b,c), 50, 125, 55, 152)", evaluateEnvironment));

        System.out.println("zasd".compareTo("50"));

        Object result = null;
        Expression expression = Expression.parse("@max(@sum(1,1,@avg(1, 2, 3, 400000 + 10000)), 3, 50, 12500, 55, -152)");
        long begin = System.currentTimeMillis();
        for(int i = 0 ; i < 100000; i ++) {
//            result = Expression.eval("@Math.pow(b, a)", evaluateEnvironment);
            result = expression.evaluate(evaluateEnvironment);
//            result = expression.eval("1 + 2 - 3 * 4 / 5 - 6 + 8 - 0");

//            result = Expression.eval("@min(9, 50, 125, 55, 152)", evaluateEnvironment);
//            result = BuiltInFunction.max(9, 50, 125, 55, 152);
//            result = Math.pow(12, 1);
        }
        long end = System.currentTimeMillis();
        System.out.println(result);
        System.out.println(" use " + (end - begin));
```

## Maven

```xml

<dependency>
    <groupId>io.github.wycst</groupId>
    <artifactId>wast</artifactId>
    <version>0.0.2</version>
</dependency>
```

## Groovy

```
implementation 'io.github.wycst:wast:0.0.2'
```

## Kotlin

```
implementation("io.github.wycst:wast:0.0.2")
```
