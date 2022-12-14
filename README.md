# wast

## 简介

一个轻量级且高性能java库。

性能评测数据 <br>
[https://github.com/wycst/wast-jmh-test](https://github.com/wycst/wast-jmh-test)

2022-09-25 json性能测试数据 <br>
[https://github.com/wycst/wast-jmh-test/blob/main/README_0925_json.md](https://github.com/wycst/wast-jmh-test/blob/main/README_0925_json.md)

## Maven

```xml

<dependency>
    <groupId>io.github.wycst</groupId>
    <artifactId>wast</artifactId>
    <version>0.0.10.1</version>
</dependency>
```

## json模块

> 1 java语言整体性能最快的json库之一；<br>
> 2 功能全面，支持IO流文件读写，JSON节点树按需解析， 局部解析，序列化格式化，驼峰下划线自动转换；<br>
> 3 源码实现简单易懂，阅读调试都很容易；<br>
> 4 代码轻量，使用安全，没有漏洞风险；<br>
> 5 兼容jdk1.6+；

## yaml模块

> 1 目前java语言解析yaml最快的库，性能大概是snakeyaml的5-20倍；<br>
> 2 支持文件流，URL, 字符数组，字符串等解析；<br>
> 3 支持常用yaml语法以及类型转换；<br>
> 4 内置Yaml节点模型，支持路径查找(v0.0.4+)；<br>
> 5 支持yaml反向转换为字符串或者文件(v0.0.4+)；<br>
> 6 兼容jdk1.6+；

## 表达式引擎

> 1 java表达式引擎，以解析性能来说比现有开源的其他引擎都快，包括mvel, spel, fel等；<br>
> 2 支持java中所有的操作运算符（加减乘除余，位运算，逻辑运算，字符串+）；<br>
> 3 支持**指数运算(java本身不支持)； <br>
> 4 支持函数以及自定义函数实现,函数可以任意嵌套； <br>
> 5 科学记数法支持，16进制，8进制等解析，支持大数运算（大数统一转为double类型）；<br>
> 6 支持简单的三目运算；<br>
> 7 代码轻量，使用安全，没有漏洞风险；<br>
> 8 支持超长文本表达式执行；<br>
> 9 支持字节码进行表达式编译；<br>
> 10 兼容jdk1.6+；

## jdbc模块

> 1 集成了类似JdbcTemplate,Mybatis-Plus或者jpa等操作习惯的api; <br>
> 2 代码轻量，没有任何代理，使用非常方便；<br>
> 3 兼容jdk1.6+；<br>

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

提供JSONReader类可按需解析一个输入流，自定义解析，可随时终止(不用将整个文件流读完)。

```
        final JSONReader reader = JSONReader.from(new File(f));
        reader.read(new JSONReader.ReaderCallback(JSONReader.ReadParseMode.ExternalImpl) {
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
> 2、支持按需解析；<br>
> 3、支持上下文查找；<br>
> 4、支持在大文本json中提取部分内容作为解析上下文结果,使用JSONNode.from(source, path, lazy?)  <br>
> 5、支持对节点的属性修改，删除等，节点的JSON反向序列化；<br>
> 6、支持直接提取功能(v0.0.2+支持)，参考JSONPath；

使用'/'作为路径的分隔符，数组下标使用[n]访问支持[*], [n+], [n-],[n]等复合下标访问，例如/store/book/[*]/author(注意不是/store/book[*]/author)

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
    
    // yaml字符串
    String yamlStr = StringUtils.fromResource("/yaml/t2.yaml");
    
    // 读取文档
    YamlDocument yamlDoc = YamlDocument.parse(yamlStr);
    
    // 转换为properties
    Properties properties = yamlDoc.toProperties();
    System.out.println(properties);
    
    // 转换为map
    yamlDoc.toMap();
    
    // 转化为指定bean
    YamlTest bean = yamlDoc.toEntity(YamlTest.class);
    
    // 获取根节点
    YamlNode yamlRoot = yamlDoc.getRoot();
    
    // 查找node
    YamlNode nameNode = yamlRoot.get("/metadata/name");
    
    // 获取/metadata/name
    String metadataName = yamlRoot.getPathValue("/metadata/name", String.class);
    // 或者 nameNode.getValue();
    System.out.println(" metadataName " + metadataName);
    
    // 修改
    yamlRoot.setPathValue("/metadata/name", "this is new Value ");
    
    String newMetadataName = (String) nameNode.getValue();
    System.out.println(newMetadataName.equals("this is new Value "));
    
    // 反向序列化生成yaml字符串
    System.out.println(yamlDoc.toYamlString());
    
    // 输出到文件
    yamlDoc.writeTo(new File("/tmp/test.yaml"));
    
```

## 表达式引擎使用

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

### 3 编译模式

使用原生java编译或者javassist（按需加载）将表达式动态编译为java类进行运行。

```
String el = "arg.a+arg.b+b+c";
CompilerEnvironment compileEnvironment = new CompilerEnvironment();

// 如果设置false会将表达式进行先解析再编译;
// 如果设置为true将跳过解析在代码中直接return，此时最好使用setVariableType来声明变量类型
// 不伦是否设置skipParse，使用setVariableType来声明变量类型都是不错的选择，能大大提高效率
compileEnvironment.setSkipParse(true);
compileEnvironment.setVariableType(int.class, "arg.a", "arg.b", "b", "c");

// 输出编译的源代码
System.out.println(CompilerExpression.generateJavaCode(el, compileEnvironment));
CompilerExpression compiler = CompilerExpression.compile(el, compileEnvironment, CompilerExpression.Coder.Native);


Map aa = new HashMap();
aa.put("a", 120);
aa.put("b", 150);

Map var = new HashMap();
var.put("arg", aa);
var.put("b", 8);
var.put("c", 1);

// 执行
System.out.println("==== eval result " + compiler.evaluate(var));

```

### 4 函数和自定义函数使用

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

        evaluateEnvironment.registerFunction("min", new ExprFunction<Object, Number>() {
            @Override
            public Number call(Object... params) {
                Arrays.sort(params);
                return (Number) params[params.length - 1];
            }
        });

        System.out.println( Expression.eval("@min(@sum(a,b,c), 50, 125, 2, -11)", evaluateEnvironment));
        System.out.println( Expression.eval("@max(@sum(a,b,c), 50, 125, 55, 152)", evaluateEnvironment));
```
## jdbc使用

 ### 构建执行器
```
 DefaultSqlExecuter sqlExecuter = new DefaultSqlExecuter();
```

### 设置数据源
```
 sqlExecuter.setDataSource(datasource); // 
```

至此可以像JdbcTemplate一样操作数据库了

### 面向sql操作

以下是sqlExecuter常用的api：

```
// 查询列表(Map)
public List<Map> queryList(final String sql, final Object... params);

// 查询列表并将封装到指定类型（E）
public <E> List<E> queryList(final String sql, final Class<E> cls, final Object... params);

// 插入操作
public Serializable insert(final String sql, final boolean returnGeneratedKeys, final Object... params) ;

// 更新操作
public int update(final String sql, final Object... params);

// 执行一个sql语句（包括ddl）
public int executeUpdate(final String sql);

// 执行一个脚本（文件，以分号结尾换行的多条sql）
public void executeScript(InputStream is) throws IOException;

// 批量操作（使用同一个sql）
sqlExecuter.executePipelined(new SqlExecuteCall<Object>() {

    @Override
    public Object execute(Connection connection) throws SQLException {
        // 使用原生的jdbc语法操作
        // 连接在这里不用关闭
        return null;
    }
});

// 分页查询
public Page<Map> queryPage(final String sql, long pageNum, int limit, final Object... params);
// 分页查询转对象
public <E> Page<E> queryPage(final String sql, long pageNum, int limit, final Class<E> cls, final Object... params);
// 根据构造好的page进行分页查询
public <E> void queryPage(Page<E> page, final String sql, final Object... params);

```

### 面向模板操作

模板语法和mybatis相似，sql中使用#{}占位替换?,使用${}占位进行值拼接

比如：
```
select * from fact where type = #{type} and name like '%${name}%'
```
在运行时将转化为(假设name=test)：
```
select * from fact where type = ? and name like '%test%'
```

通过sqlExecuter获取模板执行对象(注：sqlExecuter的大部分sql操作的api都有对应的模板api)
```
TemplateSqlExecuter templateSqlExecuter = sqlExecuter.getTemplateExecutor();

// 插入对象
Fact fact = new Fact();
fact.setId(1);
fact.setName("test");

// 插入
sqlExecuter.getTemplateExecutor().insert("insert into fact(id, name) values(#{id}, #{name})", fact);

// 查询
Fact f = sqlExecuter.getTemplateExecutor().queryObject("select * from fact where id = #{id}", 1, Fact.class);
```

### 面向对象操作

使用过jpa(hibernate)或者mybatis-plus的可以很快就上手。
使用前在前面初始化基础上需要设置实体的扫描包集合
```
 EntityManagementFactory.defaultManagementFactory().scanPackages("com.xxx.entitys", "com.xxx.entitys1");
```
> 实体不用继承任何类或实现接口，但需要添加@Table注解，并指定表名，目前不会根据类名自动推导表名;<br>
> 主键通过注解@Id来标识，支持自增（数据库自增策略），算法生成（雪花算法）以及程序代码设置等几种策略;<br>
> 其他字段通过注解@Column来映射字段名称，如果没有注解@Column，字段会默认将属性的驼峰格式转为下划线名称作为字段映射;<br>

获取实体对象操作执行者
```
EntityExecuter entityExecuter = sqlExecuter.getEntityExecuter();
```

实体对象
```
@Table(name = "t_fact")
public class Fact {
    @Id
    private String id;
   
    @Column
    private String name;
    // setter getter 省略
}

```
常用的面向对象操作
```
Fact fact = new Fact();
fact.setId(1);
fact.setName("test");

// 插入对象
entityExecuter.insert(fact);

// 更新对象
entityExecuter.update(fact);

// 根据主键查询
Fact record = entityExecuter.get(Fact.class, 1);

// 根据主键删除
entityExecuter.delete(Fact.class, 1);

// 根据条件查询
Fact param = new Fact();
param.setId(1);
List<Fact> factList = entityExecuter.queryBy(Fact.class, param)

```
更多操作可以自行发现。





