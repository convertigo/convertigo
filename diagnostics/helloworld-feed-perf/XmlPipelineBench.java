import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlPipelineBench {
    private static final int WARMUP = Integer.parseInt(System.getenv().getOrDefault("WARMUP", "10"));
    private static final int ITERATIONS = Integer.parseInt(System.getenv().getOrDefault("ITERATIONS", "100"));

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: XmlPipelineBench <rss-file>...");
        }
        System.out.println("file,bytes,items,phase,count,median_ms,mean_ms,p95_ms,min_ms,max_ms");
        for (String arg : args) {
            run(Path.of(arg));
        }
    }

    private static void run(Path path) throws Exception {
        String xml = Files.readString(path, StandardCharsets.UTF_8);
        for (int i = 0; i < WARMUP; i++) {
            pipeline(xml);
        }

        List<Double> parse = new ArrayList<>();
        List<Double> convert = new ArrayList<>();
        List<Double> sortMap = new ArrayList<>();
        List<Double> total = new ArrayList<>();
        List<Double> nasaParse = new ArrayList<>();
        List<Double> nasaExtract = new ArrayList<>();
        List<Double> nasaTotal = new ArrayList<>();
        int items = 0;
        int nasaItems = 0;

        for (int i = 0; i < ITERATIONS; i++) {
            Timings timings = pipeline(xml);
            parse.add(timings.parseMs);
            convert.add(timings.convertMs);
            sortMap.add(timings.sortMapMs);
            total.add(timings.totalMs);
            items = timings.items;
            NasaTimings exactTimings = nasaImageFeedItems(xml);
            nasaParse.add(exactTimings.parseMs);
            nasaExtract.add(exactTimings.extractMs);
            nasaTotal.add(exactTimings.totalMs);
            nasaItems = exactTimings.items;
        }

        String name = path.getFileName().toString();
        long bytes = Files.size(path);
        print(name, bytes, items, "jaxp_parse", parse);
        print(name, bytes, items, "dom_to_object", convert);
        print(name, bytes, items, "sort_map", sortMap);
        print(name, bytes, items, "total", total);
        print(name, bytes, nasaItems, "nasa_image_items_jaxp_parse", nasaParse);
        print(name, bytes, nasaItems, "nasa_image_items_extract_20", nasaExtract);
        print(name, bytes, nasaItems, "nasa_image_items_total", nasaTotal);
    }

    private static Timings pipeline(String xml) throws Exception {
        long start = System.nanoTime();
        Document document = parseXml(xml);
        long afterParse = System.nanoTime();
        Map<String, Object> value = documentValue(document);
        long afterConvert = System.nanoTime();
        List<Map<String, Object>> rows = sortAndMap(value);
        long afterSortMap = System.nanoTime();
        return new Timings(
            millis(afterParse - start),
            millis(afterConvert - afterParse),
            millis(afterSortMap - afterConvert),
            millis(afterSortMap - start),
            rows.size()
        );
    }

    private static Document parseXml(String text) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        tryFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        tryFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        tryFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        tryFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        try {
            factory.setXIncludeAware(false);
        } catch (UnsupportedOperationException ignored) {
        }
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(text)));
    }

    private static NasaTimings nasaImageFeedItems(String xml) throws Exception {
        long start = System.nanoTime();
        Document document = parseNasaXml(xml);
        long afterParse = System.nanoTime();
        List<Map<String, String>> items = extractNasaImageItems(document, 20);
        long afterExtract = System.nanoTime();
        return new NasaTimings(
            millis(afterParse - start),
            millis(afterExtract - afterParse),
            millis(afterExtract - start),
            items.size()
        );
    }

    private static Document parseNasaXml(String text) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        tryFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        tryFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        tryFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(text)));
    }

    private static List<Map<String, String>> extractNasaImageItems(Document document, int limit) {
        NodeList nodes = document.getElementsByTagName("item");
        List<Map<String, String>> items = new ArrayList<>();
        for (int i = 0; i < nodes.getLength() && items.size() < limit; i++) {
            Element item = (Element) nodes.item(i);
            String title = firstText(item, "title");
            String description = firstText(item, "description");
            String imageUrl = firstAttribute(item, "enclosure", "url");
            if (imageUrl.isEmpty()) {
                imageUrl = firstAttribute(item, "media:content", "url");
            }
            if (imageUrl.isEmpty()) {
                imageUrl = firstAttribute(item, "media:thumbnail", "url");
            }
            if (!title.isEmpty() || !description.isEmpty() || !imageUrl.isEmpty()) {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("title", title);
                row.put("description", description);
                row.put("imageUrl", imageUrl);
                items.add(row);
            }
        }
        return items;
    }

    private static String firstText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes == null || nodes.getLength() == 0) {
            return "";
        }
        String text = nodes.item(0).getTextContent();
        return text == null ? "" : text.trim();
    }

    private static String firstAttribute(Element element, String tagName, String attrName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes == null || nodes.getLength() == 0) {
            return "";
        }
        String value = ((Element) nodes.item(0)).getAttribute(attrName);
        return value == null ? "" : value.trim();
    }

    private static void tryFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignored) {
        }
    }

    private static Map<String, Object> documentValue(Document document) {
        Map<String, Object> output = new LinkedHashMap<>();
        Element root = document.getDocumentElement();
        output.put(root.getNodeName(), elementValue(root));
        return output;
    }

    private static Object elementValue(Element element) {
        Map<String, Object> output = new LinkedHashMap<>();
        NamedNodeMap attrs = element.getAttributes();
        if (attrs != null && attrs.getLength() > 0) {
            Map<String, Object> attrMap = new LinkedHashMap<>();
            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);
                attrMap.put(attr.getNodeName(), attr.getNodeValue());
            }
            output.put("attr", attrMap);
        }

        StringBuilder text = new StringBuilder();
        boolean hasElement = false;
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            short type = child.getNodeType();
            if (type == Node.ELEMENT_NODE) {
                hasElement = true;
                addChild(output, child.getNodeName(), elementValue((Element) child));
            } else if (type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE) {
                text.append(child.getNodeValue());
            }
        }

        String trimmed = text.toString().trim();
        if (!hasElement && !output.containsKey("attr")) {
            return trimmed;
        }
        if (!trimmed.isEmpty() || (!hasElement && output.containsKey("attr"))) {
            output.put("text", trimmed);
        }
        return output;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> sortAndMap(Map<String, Object> document) {
        Map<String, Object> rss = (Map<String, Object>) document.get("rss");
        Map<String, Object> channel = (Map<String, Object>) rss.get("channel");
        Object itemValue = channel.get("item");
        List<Object> items = itemValue instanceof List<?> list ? new ArrayList<>(list) : new ArrayList<>(List.of(itemValue));
        items.sort(Comparator.comparing(item -> stringAt(item, "title")));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("title", stringAt(item, "title"));
            row.put("description", stringAt(item, "description"));
            row.put("imageUrl", stringAt(item, "enclosure", "attr", "url"));
            row.put("link", stringAt(item, "link"));
            row.put("pubDate", stringAt(item, "pubDate"));
            rows.add(row);
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private static String stringAt(Object source, String... path) {
        Object cursor = source;
        for (String key : path) {
            if (!(cursor instanceof Map<?, ?> map)) {
                return "";
            }
            cursor = ((Map<String, Object>) map).get(key);
        }
        return cursor == null ? "" : String.valueOf(cursor);
    }

    private static void addChild(Map<String, Object> target, String key, Object value) {
        Object existing = target.get(key);
        if (existing == null) {
            target.put(key, value);
        } else if (existing instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Object> writable = (List<Object>) list;
            writable.add(value);
        } else {
            List<Object> values = new ArrayList<>();
            values.add(existing);
            values.add(value);
            target.put(key, values);
        }
    }

    private static void print(String name, long bytes, int items, String phase, List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        System.out.printf(
            "%s,%d,%d,%s,%d,%.3f,%.3f,%.3f,%.3f,%.3f%n",
            name,
            bytes,
            items,
            phase,
            sorted.size(),
            median(sorted),
            sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0),
            sorted.get((int) Math.round((sorted.size() - 1) * 0.95)),
            sorted.get(0),
            sorted.get(sorted.size() - 1)
        );
    }

    private static double median(List<Double> sorted) {
        int size = sorted.size();
        if (size % 2 == 1) {
            return sorted.get(size / 2);
        }
        return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
    }

    private static double millis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private record Timings(double parseMs, double convertMs, double sortMapMs, double totalMs, int items) {
    }

    private record NasaTimings(double parseMs, double extractMs, double totalMs, int items) {
    }
}
