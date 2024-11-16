package com.wast.wiki.json;

import com.wast.wiki.beans.Book;
import io.github.wycst.wast.common.utils.StringUtils;
import io.github.wycst.wast.json.*;
import io.github.wycst.wast.json.options.WriteOption;

import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;

/**
 * @Date 2024/11/15 0:44
 * @Created by wangyc
 */
public class JSONNodeExample {

    static String source;

    static {
        source = StringUtils.fromResource("/json/path.json");
        System.out.println(source);
    }

    public static void main(String[] args) throws Exception {
        jsonNodeNormal();
        jsonNodeEnhanced();
        jsonNodeXpath();
        jsonNodeCode();
    }

    /**
     * 1.普通场景
     *
     * @throws Exception
     */
    private static void jsonNodeNormal() throws Exception {
        System.out.println("===============jsonNodeNormal==================================");
        JSONNode root = JSONNode.parse(source);

        // 通过getPathValue方法可以获取任意路径上的值
        // 1.直接通过根节点（不限于根节点）获取第一本书的价格
        double price = root.getPathValue("/store/book/0/price", double.class);
        System.out.println("price " + price);

        // 2.获取第一本书并转化为Book对象
        Book book = root.getPathValue("/store/book/0", Book.class);
        System.out.println("book[0]: \n" + JSON.toJsonString(book));

        // 3.定位到指定节点book(数组)
        JSONNode booksNode = root.get("/store/book");
        // 或者使用getByPaths更高效
        JSONNode booksNode2 = root.getByPaths("store", "book");

        // 4.获取book数量
        int count = booksNode.getElementCount();
        System.out.println("count: " + count);

        // 5.获取第2本书的节点模型
        JSONNode bookTwo = booksNode.getElementAt(1); // or booksNode.get("0");
        System.out.println("book[1]: \n" + bookTwo.toJsonString());

        // 6.获取第2本书的作者
        System.out.println("book[1]-author: " + bookTwo.getChildValue("author", String.class));

        // 7.修改第二本书的作者
        bookTwo.setChildValue("author", "lilei");
        // 或者根据绝对路径修改
        root.setPathValue("/store/book/1/author", "lilei"); // 暂不支持不存在创建

        // 8.修改第二本书的position,第三个参数设置true当属性position不存在时创建position属性
        bookTwo.setChildValue("position", "position1", true);

        // 9.修改某个节点的value会改变当前节点的类型和结构
        bookTwo.setValue(new HashMap());

        // 10.将节点转化为json字符串
        String bookJson = bookTwo.toJsonString(); // 或者使用bookTwo.toJsonString(true)
        System.out.println("bookJson: " + bookJson);

        // 11.将更新后的节点写入到文件
        Writer writer = new FileWriter("e:/tmp/1.txt");
        root.writeTo(writer);
    }

    /**
     * 2.增强模式
     */
    private static void jsonNodeEnhanced() {
        System.out.println("===============jsonNodeEnhanced==================================");
        // 从源字符串中提取子串作为根节点，在大文本下能大幅度提高性能和降低内存
        // 1.提取第一本书的路径作为根节点
        JSONNode bookOne = JSONNode.from(source, "/store/book/0");
        System.out.println(bookOne.toJsonString(true));

        // 2.直接提取第一本书并转化为Book对象
        Book book = JSONNode.from(source, "/store/book/0", Book.class);
        System.out.println(JSON.toJsonString(book, WriteOption.FormatOutColonSpace));
    }

    /**
     * 2.xpath支持collect（注递归使用双杠//否则使用单杠/）
     *
     * @see io.github.wycst.wast.json.JSONNodePath
     */
    private static void jsonNodeXpath() {
        System.out.println("===============jsonNodeXpath==================================");
        // 以下所有的案例如要支持递归将/换成双杠//即可
        // 1.获取第1本书
        List<JSONNode> firstBookNodes = JSONNode.collect(source, "/store/book/0");
        System.out.println(firstBookNodes);
        // 2.获取最后1本书
        List<JSONNode> lastBookNodes = JSONNode.collect(source, "/store/book/-1");
        System.out.println(lastBookNodes);
        // 3.获取第2和第3等两本书，可以使用JSONNodeCollector.ANY将转化为java对象
        List<Object> rangeBooks = JSONNode.collect(source, "/store/book/2~3", JSONNodeCollector.ANY);
        System.out.println(rangeBooks);
        // 4.获取所有书，并转化为Book列表
        List<Book> allBooks = JSONNode.collect(source, "/store/book/*", JSONNodeCollector.of(Book.class));
        System.out.println(allBooks);
        // 5.获取作者J. R. R. Tolkien的书
        List<JSONNode> filterBooks = JSONNode.collect(source, "/store/book/*[author=='J. R. R. Tolkien']");
        System.out.println(JSON.toJsonString(filterBooks));
        // 6.获取作者J. R. R. Tolkien的书的价格
        double price = JSONNode.first(source, "/store/book/*[author=='J. R. R. Tolkien']/price", double.class);
        System.out.println(price);
        // 7.获取作者Herman Melville的同时价格小于10(多条件过滤),过滤表达式参考Expression语法
        Book book = JSONNode.first(source, "/store/book/*[author=='Herman Melville'&&price<10]", Book.class);
        System.out.println(JSON.toJsonString(book));
        Book book2 = JSONNode.firstIfEmpty(source, "/store/book/*[author=='Herman Melville'&&price>=10]", Book.class, null);
        System.out.println(book2); // NULL
        // 8.获取价格最高的一本书的书名（注意仅限于在某一个集合下），注意引入了parent内置的变量
        String bookName = JSONNode.first(source, "/store/book/*[price==parent.max('price')]/title", String.class);
        System.out.println(bookName);
        // 9.获取存在isbn属性的书名
        System.out.println(JSONNode.collect(source, "/store/book/*[isbn]/title"));
        // 10.获取所有的book节点
        System.out.println(JSONNode.collect(source, "//book/*"));

        //注以上案例都是从数据源直接提取目标场景，同时也支持在已经构建的节点上运行提取
        JSONNode root = JSONNode.parse(source);
        root.collect("/store/book/0");
    }

    /**
     * 3.JSONNodePath支持collect
     *
     * <p> xpath字符串会转化为JSONNodePath对象
     * <p> 使用编程式构建JSONNodePath对象可以减少xpath的解析（虽然解析很快），效率更高
     * <p> 支持自定义构建，过滤提取更灵活
     *
     * @see io.github.wycst.wast.json.JSONNodePath
     */
    private static void jsonNodeCode() {
        String xpath = "/store/book/0";
        // 通过解析xpath
        JSONNodePath path = JSONNodePath.parse(xpath);
        // 通过编程构建方式1（不支持递归）
        JSONNodePath path1 = JSONNodePath.paths("store", "book", 0);
        xpath = "/store//book/0";
        // 通过编程构建方式2
        JSONNodePath path2 = JSONNodePath.create().exact("store").exact("book", true).exact(0);
        // 通过编程构建方式3(最灵活自由的方式)
        JSONNodePath path3 = JSONNodePath.collectors(
                JSONNodePathCollector.exact("store"),
                JSONNodePathCollector.exact("book").recursive(true),
                JSONNodePathCollector.exact(0)
        );

        // 1.匹配前缀
        xpath = "/name*";
        JSONNodePath.create().prefix("name");

        // 2.匹配后缀
        xpath = "/*name";
        JSONNodePath.create().suffix("name");

        // 3.匹配包含
        xpath = "/*name*";
        JSONNodePath.create().contains("name");

        // 4.正则表达式(xpath构建请省略正则表达式中//，如果有一定要加类似/gi等请使用编程式构建)
        xpath = "/^.*name.*";
        JSONNodePath.create().regular(".*name.*");

        // 5.任意匹配
        xpath = "/*";
        JSONNodePath.create().any();

        // 7.数组下标/对象属性
        xpath = "/3";
        xpath = "/name";
        xpath = "/-1"; // 倒数第一个
        xpath = "/-2"; // 倒数第二个
        JSONNodePath.create().exact(3);
        JSONNodePath.create().exact("name");
        JSONNodePath.create().exact(-1);
        JSONNodePath.create().exact(-2);

        // 8.匹配前n个或者后n个（数组）
        xpath = "/3-";    // 前4个
        xpath = "/-3+";   // 后3个
        JSONNodePath.create().le(3);   // 小于等于3(0,1,2,3)
        JSONNodePath.create().ge(-3);  // >= -3(倒数 -1, -2, -3)

        // 9.匹配范围（数组）
        xpath = "/2~5";    // 2~5(2,3,4,5)
        xpath = "/2~-1";    // 2~-1(2,3,4... -1（倒数第一）)
        JSONNodePath.create().range(2, -1);

        // 10.离线下标集合(数组)
        xpath = "/[1,2,5]";  // 注意[]不能省略
        JSONNodePath.create().indexs(1, 2, 5);

        // 11.过滤表达式
        xpath = "p1[a + b > c]//p2[x == 'a']/p3[y.contains('u')]";
        JSONNodePath r = JSONNodePath.collectors(
                JSONNodePathCollector.exact("p1").condition("a + b > c"),
                JSONNodePathCollector.exact("p2").recursive(true).condition("x == 'a'"),
                JSONNodePathCollector.exact("p3").condition("y.contains('u')")   // 表达式支持所有java对象的方法调用
        );
        System.out.println(r.toString().equals(JSONNodePath.parse(xpath).toString()));
    }

}
