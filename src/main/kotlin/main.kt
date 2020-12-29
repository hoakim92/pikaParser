import org.jsoup.Jsoup
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.*
import java.util.stream.Collectors
import javax.xml.parsers.DocumentBuilderFactory

fun main(args: Array<String>) {
    val response = getHTML("https://pikabu.ru/story/7_sluchaev_kogda_vaktsinatsiya_zakonchilas_provalom_7922046")
    val article = getArticle(response)
    val content = getContent(article)
    val title = getTitle(article)
    val author = getAuthor(article)


//    println(author)
//    response.select(".story__main")
//    println(response.select("div.story__main").first())
//    for (r in response.select("div.story__main"))
//    {
//        println(r)
//    }
//    println(response)
//    val response = getSitemap("https://pikabu.ru/sitemap_index.xml")
//    val elements = getElements("sitemap", parseDocument(response))
//    val conn = getConnection()



//    truncateTable(conn, "story")
//
//    getElements(
//        "url",
//        DocumentBuilderFactory.newInstance().newDocumentBuilder()
//            .parse(InputSource(StringReader(getSitemap("https://pikabu.ru/sitemap/story/143.xml"))))
//    ).forEach { l -> insertStory(getConnection(), l) }

//    sortElements(elements.map { it["loc"]!! }).filter { it.key == "story" }.forEach {
//        println(it.key)
//        it.value.forEach { u ->
//            Thread.sleep(5000)
//            println(getSitemap(u))
//            getElements(
//                "url",
//                DocumentBuilderFactory.newInstance().newDocumentBuilder()
//                    .parse(InputSource(StringReader(getSitemap(u))))
//            ).forEach { l -> insertStory(getConnection(), l) }
//        }
//    }
//    conn.close()
}

fun getArticle(doc: org.jsoup.nodes.Document): org.jsoup.nodes.Element {
    return doc.select("div.story__main").first()
}

fun getContent(el: org.jsoup.nodes.Element): String {
    return el.select("div.story__content-inner").first().toString()
}

fun getTitle(el: org.jsoup.nodes.Element) : String {
    return el.select("span.story__title-link").first().text()
}

fun getAuthor(el: org.jsoup.nodes.Element) : String {
    return el.select("div.story__user-info > a").last().text()
}

fun truncateTable(connection: Connection, table: String) {
    insertRow(connection, "TRUNCATE TABLE pikabuparser.$table")
}

fun insertStoryDetail(connection: Connection, row: Map<String, Any>) {
    val sql = "INSERT INTO pikabuparser.story (loc, changefreq, priority) VALUES ('${row["loc"]!!}', '${row["changefreq"]!!}', ${row["priority"]!!})"
    insertRow(connection,sql)
}

fun insertStory(connection: Connection, row: Map<String, Any>) {
    val sql = "INSERT INTO pikabuparser.story (loc, changefreq, priority) VALUES ('${row["loc"]!!}', '${row["changefreq"]!!}', ${row["priority"]!!})"
    insertRow(connection,sql)
}

fun insertRow(connection: Connection, sql: String) {
    with(connection) {
        println(sql)
        createStatement().execute(sql)
    }
}

fun resultSetToArrayList(rs: ResultSet): List<Map<String, String>> {
    val md = rs.metaData
    val list = mutableListOf<Map<String, String>>()
    while (rs.next()) {
        val row = mutableMapOf<String, String>()
        for (i in 1..md.columnCount) {
            row[md.getColumnName(i)] = rs.getObject(i).toString()
        }
        list.add(row);
    }
    return list;
}

fun executeQuery(query: String, conn: Connection): ResultSet {
    println(query)
    return conn!!.createStatement().executeQuery(query)
}

fun getConnection(): Connection {
    val connectionProps = Properties()
    connectionProps["user"] = "admin"
    connectionProps["password"] = "admin"
    return DriverManager.getConnection(
        "jdbc:mysql://127.0.0.1:3306/?useUnicode=true&serverTimezone=UTC",
        connectionProps
    )
}


fun sortElements(elements: List<String>): Map<String, List<String>> {
    println("Start sort elements")
    return elements.groupBy { it.subSequence(26, it.indexOf("/", 26)).toString() }
}

fun getElements(tag: String, xml: Document): List<Map<String, String>> {
    println("Start getting elements by tag $tag")
    val nodes = xml.getElementsByTagName(tag)
    val elements = mutableListOf<Map<String, String>>()
    for (i in 0 until nodes.length) {
        elements.add(convertNodeToMap(nodes.item(i)))
    }
    return elements
}

fun convertNodeToMap(node: Node, getChildren: Boolean = true): Map<String, String> {
    val m = mutableMapOf<String, String>()
    if (getChildren) {
        for (i in 0 until node.childNodes.length) {
            m[node.childNodes.item(i).nodeName] = node.childNodes.item(i).firstChild.nodeValue
        }
    } else {
        for (i in 0 until node.attributes.length) {
            m[node.attributes.item(i).nodeName] = node.attributes.item(i).nodeValue
        }
    }
    return m
}


fun parseDocument(str: String): Document {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(str)))
}

fun getHTML(url: String): org.jsoup.nodes.Document {
    return Jsoup.connect(url).get()
}

fun getSitemap(url: String): String {
    println("Start request to $url")
    with(URL(url).openConnection() as HttpURLConnection) {
        setRequestProperty("Content-Type", "application/xml");
        setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (X11; U; Linux i586; en-US; rv:1.7.3) Gecko/20040924 Epiphany/1.4.4 (Ubuntu)"
        )
        return inputStream.bufferedReader().lines().collect(Collectors.toList()).joinToString(" /n")
    }
}