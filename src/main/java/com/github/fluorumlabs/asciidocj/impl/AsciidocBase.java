package com.github.fluorumlabs.asciidocj.impl;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.fluorumlabs.asciidocj.impl.Utils.moveChildNodes;
import static com.github.fluorumlabs.asciidocj.impl.Utils.skipRight;
import static com.github.fluorumlabs.asciidocj.impl.Utils.stripTail;

/**
 * Created by Artem Godin on 11/27/2018.
 */
public abstract class AsciidocBase {
    protected Document document;
    protected StringBuilder textBuilder = new StringBuilder(256);
    protected Element currentElement;

    protected JSONObject properties = new JSONObject();
    protected JSONObject currentProperties = new JSONObject();
    protected JSONObject attributes = new JSONObject();

    // We don't need that :)
    protected static class Yytoken {
    }

    private void propagateProperties(Element element) {
        if (!properties.keySet().isEmpty()) {
            if (properties.has("class")) {
                for (String aClass : properties.getJSONObject("class").keySet()) {
                    element.addClass(aClass);
                }
            }
            if (properties.has("id")) {
                element.attr("id", properties.getString("id"));
            }
        }
        currentProperties = properties;
        properties = new JSONObject();
    }

    protected void promoteArgumentsToClasses() {
    if (properties.has("arguments") && properties.getJSONArray("arguments").length()==1) {
        if ( !properties.has("class") ) {
            properties.put("class", new JSONObject());
        }
        JSONObject classes = properties.getJSONObject("class");

        for (String aClass : properties.getJSONArray("arguments").getString(0).split(" ")) {
            classes.put(aClass,"");
        }
    }
    }

    protected String getArgument(int i) {
        return getArgument(properties,i);
    }

    protected boolean hasClass(String x) {
        if (!properties.has("class")) {
            return false;
        } else {
            return properties.getJSONObject("class").has(x);
        }
    }

    protected String getArgument(JSONObject properties, int i) {
        if (!properties.has("arguments")) {
            return "";
        } else {
            JSONArray arguments = properties.getJSONArray("arguments");
            return arguments.optString(i, "");
        }
    }

    protected boolean hasOption(String key) {
        return hasOption(key, properties);
    }

    protected boolean hasOption(String key, JSONObject properties) {
        if (!properties.has("options")) {
            return false;
        } else {
            return properties.getJSONObject("options").has(key);
        }
    }

    protected void appendText(String string) {
        if (string.endsWith("\0")) {
            textBuilder.append(stripTail(string, 1));
        } else {
            textBuilder.append(string);
        }
    }

    protected void clearText() {
        textBuilder.setLength(0);
    }

    protected String getText() {
        return textBuilder.toString();
    }

    protected String getTextAndClear() {
        String result = getText();
        clearText();
        return result;
    }

    protected void appendTextNode() {
        appendTextNode(false);
    }

    protected void appendTextNode(boolean stripNewLines) {
        String text = getText();
        if (stripNewLines) text = skipRight(text, " \t\n\0");
        if (!text.isEmpty()) {
            currentElement.appendChild(new TextNode(text, ""));
            clearText();
        }
    }

    protected void appendDocument(Document document) {
        moveChildNodes(document.body(), currentElement);
    }

    protected Element openElement(String tag) {
        appendTextNode();
        Element newElement = new Element(Tag.valueOf(tag), "");
        currentElement.appendChild(newElement);
        currentElement = newElement;
        propagateProperties(newElement);
        return newElement;
    }

    protected Element openElement(AsciidocRenderer tag) {
        appendTextNode();
        Element newElement = new AsciidocElement(tag, properties, attributes);
        currentElement.appendChild(newElement);
        currentElement = newElement;
        propagateProperties(newElement);
        return newElement;
    }

    protected boolean openOrCloseElement(String tag) {
        Element temp = currentElement;
        closeElement(tag);
        if (currentElement == temp) {
            openElement(tag);
            return true;
        }
        return false;
    }

    protected Element closeElement(String... tag) {
        appendTextNode();
        Set<String> tags = new HashSet<>(Arrays.asList(tag));
        getParents(currentElement).stream()
                .filter(e -> tags.contains(e.tagName()))
                .findFirst()
                .ifPresent(e -> currentElement = e.parent());

        return currentElement;
    }

    protected Element closeElement(AsciidocRenderer... tag) {
        appendTextNode();
        Set<String> tags = Arrays.stream(tag)
                .map(AsciidocRenderer::tag)
                .collect(Collectors.toSet());
        getParents(currentElement).stream()
                .filter(e -> tags.contains(e.tagName()))
                .findFirst()
                .ifPresent(e -> currentElement = e.parent());

        return currentElement;
    }

    protected Element closeElementTop(AsciidocRenderer... tag) {
        appendTextNode();
        Set<String> tags = Arrays.stream(tag)
                .map(AsciidocRenderer::tag)
                .collect(Collectors.toSet());
        getParentsReversed(currentElement).stream()
                .filter(e -> tags.contains(e.tagName()))
                .findFirst()
                .ifPresent(e -> currentElement = e.parent());

        return currentElement;
    }

    protected Element closeToElement(AsciidocRenderer... tag) {
        appendTextNode();
        Set<String> tags = Arrays.stream(tag)
                .map(AsciidocRenderer::tag)
                .collect(Collectors.toSet());
        getParents(currentElement).stream()
                .filter(e -> tags.contains(e.tagName()))
                .findFirst()
                .ifPresent(e -> currentElement = e);

        return currentElement;
    }

    protected boolean isInside(AsciidocRenderer... tag) {
        appendTextNode();
        Set<String> tags = Arrays.stream(tag)
                .map(AsciidocRenderer::tag)
                .collect(Collectors.toSet());
        return getParents(currentElement).stream()
                .anyMatch(e -> tags.contains(e.tagName()));
    }

    protected Element closeElement(AsciidocRenderer tag, int level) {
        appendTextNode();
        String value = Integer.toString(level);
        getParents(currentElement).stream()
                .filter(e -> tag.tag().equals(e.tagName()) && e.attr("level").equals(value))
                .findFirst()
                .ifPresent(e -> currentElement = e.parent());

        return currentElement;
    }

    protected Element closeElement() {
        appendTextNode();
        currentElement = currentElement.parent();
        return currentElement;
    }

    protected Element closeToElement(AsciidocRenderer tag, int level) {
        appendTextNode();
        String value = Integer.toString(level);
        getParents(currentElement).stream()
                .filter(e -> tag.tag().equals(e.tagName()) && e.attr("level").equals(value))
                .findFirst()
                .ifPresent(e -> currentElement = e);

        return currentElement;
    }

    protected Element closeBlockElement() {
        appendTextNode(true);
        getParents(currentElement).stream()
                .filter(e -> e.tagName().endsWith("_BLOCK__"))
                .findFirst()
                .ifPresent(e -> currentElement = e.parent());

        return currentElement;
    }

    protected Element appendElement(String tag) {
        appendTextNode();
        Element appendedElement = new Element(Tag.valueOf(tag), "");
        currentElement.appendChild(appendedElement);
        return appendedElement;
    }

    private static List<Element> getParents(Element element) {
        List<Element> parents = new ArrayList<>();
        Element body = element.ownerDocument().body();
        while (element != null && element != body) {
            parents.add(element);
            element = element.parent();
        }
        return parents;
    }

    private List<Element> getParentsReversed(Element element) {
        List<Element> parents = new ArrayList<>();
        while (element != null && element != element.ownerDocument().body()) {
            parents.add(0, element);
            element = element.parent();
        }
        return parents;
    }


    /* The working horse */

    protected void enrich() {
        Elements allElements = document.getAllElements();
        for (Element x : allElements) {
            if (x instanceof AsciidocElement) {
                AsciidocElement xx = (AsciidocElement) x;
                xx.process();
            }
        }
        document.select("[properties]").removeAttr("properties");
        document.select("mark[class], mark[id]").tagName("span");

        // Preamble postprocessing
        boolean isFirst = true;
        for (Element preamble : document.select("div#preamble")) {
            if (preamble != null && preamble.text().isEmpty()) {
                preamble.remove();
            } else if (preamble != null && (document.select("h2,h3,h4,h5,h6").isEmpty() || !isFirst)) {
                Element section = preamble.select("div.sectionbody").first();
                if (section != null) {
                    List<Node> nodes = new ArrayList<>(section.childNodes());
                    for (Node node : nodes) {
                        preamble.before(node);
                    }
                }
                preamble.remove();
            }
            isFirst = false;
        }
    }
}