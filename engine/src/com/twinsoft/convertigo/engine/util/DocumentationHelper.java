/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

/**
 * Utility methods to consistently handle Convertigo bean short descriptions.
 * <p>
 * A raw short description follows the historic format "short | long" where the
 * short part is a concise summary and the long part contains the extended
 * documentation written in HTML.
 */
public final class DocumentationHelper {

    private static final Pattern BREAK_TAG = Pattern.compile("<br\\s*/?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARAGRAPH_TAG = Pattern.compile("</?p>", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIST_TAG = Pattern.compile("</?(ul|ol)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern NOTE_PARAGRAPH_PATTERN = Pattern.compile("(?i)<p><strong>Note:</strong>(.*?)</p>", Pattern.DOTALL);
    private static final Pattern NOTE_FOLLOWING_BLOCK_PATTERN = Pattern.compile("\\s*(<(?:p|ul|ol)\\b.*?</(?:p|ul|ol)>)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern NOTE_CONTINUATION_BLOCK_PATTERN = Pattern.compile("(?is)^<(p|ul|ol)\\b.*</\\1>$");
    private static final Pattern DOC_NOTE_HTML_PATTERN = Pattern.compile(
        "(?i)<blockquote class=\"doc-note\">(.*?)</blockquote>",
        Pattern.DOTALL);

    private static final String CONTEXT_MENU_SCRIPT = """
            document.oncontextmenu = new Function("return false");
            """;

    private static final String COMMON_STYLE = """
            html {
              padding: 0; margin: 0; border-left: lightgrey solid 2px;
            }
            body {
              font-family: 'Inter', sans-serif;
              font-size: 14px;
              padding-left: .3em;
              color: $foreground$;
              background-color: $background$;
              -webkit-font-smoothing: antialiased;
            }
            a { color: $link$; text-decoration: none; }
            a:hover { text-decoration: underline; }
            code {
              font-family: 'JetBrains Mono','Courier New',monospace;
              background: rgba(0,183,245,.4);
              border: 1px solid #00b7f5;
              padding: 0 4px;
              border-radius: 4px;
              font-size: .95em;
            }
            strong { font-weight: 800; }
            em, i {
              font-style: italic;
              font-weight: 500;
              padding: 0 2px;
              border-radius: 3px;
              text-shadow: #00b7f5 1px 0 1px;
            }
            p { margin: 0 0 .6em 0; line-height: 1.55; }
            ul { margin: 0 0 .6em 1.2em; padding-left: 1.2em; }
            li { margin-bottom: .35em; }
            blockquote.doc-note {
              border-left: 4px solid #00b7f5;
              background: rgba(0,183,245,.4);
              padding: .55em .85em;
              margin: 0 0 .85em 0;
              border-radius: 4px;
              color: inherit;
            }
            blockquote.doc-note p { margin: 0 0 .4em 0; }
            blockquote.doc-note p:last-child { margin-bottom: 0; }
            """;

    private DocumentationHelper() {
        // utility class
    }

    public static Documentation parse(String rawDescription) {
        if (rawDescription == null) {
            return Documentation.EMPTY;
        }
        var trimmed = rawDescription.trim();
        if (trimmed.isEmpty()) {
            return Documentation.EMPTY;
        }
        var shortPart = trimmed;
        var longPart = StringUtils.EMPTY;
        var pipeIndex = trimmed.indexOf('|');
        if (pipeIndex >= 0) {
            shortPart = trimmed.substring(0, pipeIndex);
            longPart = trimmed.substring(pipeIndex + 1);
        }
        return new Documentation(shortPart.trim(), longPart.trim());
    }

    public static String shortDescription(String rawDescription, boolean asHtml) {
        var documentation = parse(rawDescription);
        return asHtml ? documentation.getShortHtml() : documentation.getShortText();
    }

    public static String longDescription(String rawDescription, boolean asHtml) {
        var documentation = parse(rawDescription);
        return asHtml ? documentation.getLongHtml() : documentation.getLongText();
    }

    public static String fullDescription(String rawDescription, boolean asHtml) {
        var documentation = parse(rawDescription);
        if (asHtml) {
            if (documentation.getLongHtml().isEmpty()) {
                return documentation.getShortHtml();
            }
            return documentation.getShortHtml() + "<br/><br/>" + documentation.getLongHtml();
        }
        if (documentation.getLongText().isEmpty()) {
            return documentation.getShortText();
        }
        return documentation.getShortText() + System.lineSeparator() + System.lineSeparator() + documentation.getLongText();
    }

    public static String shortDescriptionMarkdown(String rawDescription) {
        return parse(rawDescription).getShortMarkdown();
    }

    public static String longDescriptionMarkdown(String rawDescription) {
        return parse(rawDescription).getLongMarkdown();
    }

    public static String fullDescriptionMarkdown(String rawDescription) {
        var documentation = parse(rawDescription);
        var shortMd = documentation.getShortMarkdown();
        var longMd = documentation.getLongMarkdown();

        if (StringUtils.isBlank(shortMd)) {
            return longMd;
        }
        if (StringUtils.isBlank(longMd)) {
            return shortMd;
        }
        return shortMd + System.lineSeparator() + System.lineSeparator() + longMd;
    }

    public static final class Documentation {
        private static final Documentation EMPTY = new Documentation(StringUtils.EMPTY, StringUtils.EMPTY);

        private final String shortHtml;
        private final String longHtml;
        private final String shortText;
        private final String longText;
        private final String shortMarkdown;
        private final String longMarkdown;

        private Documentation(String rawShortHtml, String rawLongHtml) {
            Objects.requireNonNull(rawShortHtml, "shortHtml");
            Objects.requireNonNull(rawLongHtml, "longHtml");

            this.shortHtml = normalizeShortHtml(rawShortHtml);
            this.shortMarkdown = inlineHtmlToMarkdown(this.shortHtml);

            var longFormat = normalizeLong(rawLongHtml);
            this.longHtml = longFormat.html;
            this.longMarkdown = longFormat.markdown;
            this.shortText = htmlToPlainText(this.shortHtml);
            this.longText = htmlToPlainText(this.longHtml);
        }

        public String getShortHtml() {
            return shortHtml;
        }

        public String getLongHtml() {
            return longHtml;
        }

        public String getShortText() {
            return shortText;
        }

        public String getLongText() {
            return longText;
        }

        public String getShortMarkdown() {
            return shortMarkdown;
        }

        public String getLongMarkdown() {
            return longMarkdown;
        }
    }

    private static String htmlToPlainText(String html) {
        if (StringUtils.isBlank(html)) {
            return StringUtils.EMPTY;
        }
        var text = html;
        text = BREAK_TAG.matcher(text).replaceAll(System.lineSeparator());
        text = PARAGRAPH_TAG.matcher(text).replaceAll(System.lineSeparator());
        text = LIST_TAG.matcher(text).replaceAll(System.lineSeparator());
        text = text.replaceAll("(?i)<li>", " - ");
        text = text.replaceAll("(?i)</li>", System.lineSeparator());
        text = TAG_PATTERN.matcher(text).replaceAll(StringUtils.EMPTY);
        text = Strings.CS.replace(text, "&nbsp;", " ");
        text = Strings.CS.replace(text, "&lt;", "<");
        text = Strings.CS.replace(text, "&gt;", ">");
        text = Strings.CS.replace(text, "&amp;", "&");
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    private static String normalizeShortHtml(String shortHtml) {
        var sanitized = sanitizeInlineHtml(shortHtml);
        return sanitized.trim();
    }

    private static LongFormat normalizeLong(String longHtml) {
        if (StringUtils.isBlank(longHtml)) {
            return new LongFormat(StringUtils.EMPTY, StringUtils.EMPTY);
        }

        var sanitized = sanitizeInlineHtml(longHtml);
        var html = wrapNotesHtml(sanitized.trim());
        if (html.isEmpty()) {
            return new LongFormat(StringUtils.EMPTY, StringUtils.EMPTY);
        }
        var markdown = htmlBlockToMarkdown(html);
        return new LongFormat(html, markdown);
    }

    private static String sanitizeInlineHtml(String html) {
        if (StringUtils.isBlank(html)) {
            return StringUtils.EMPTY;
        }
        var sanitized = html.replace("\r\n", "\n").replace("\r", "\n");
        sanitized = sanitized.replace("\\u2022", "•").replace('\u2022', '•').replace("&#8226;", "•");
        sanitized = sanitized.replace("<br />", "<br/>").replace("</br>", "<br/>");

        sanitized = replaceSpanBlocks(sanitized, "orangetwinsoft", "strong");
        sanitized = replaceSpanBlocks(sanitized, "computer", "code");

        // Remove any unmatched span remnants
        sanitized = sanitized.replace("<span class=\"orangetwinsoft\">", "")
                             .replace("<span class='orangetwinsoft'>", "")
                             .replace("<span class=\\\"orangetwinsoft\\\">", "")
                             .replace("<span class=\"computer\">", "")
                             .replace("<span class='computer'>", "")
                             .replace("<span class=\\\"computer\\\">", "")
                             .replace("</span>", "");

        sanitized = sanitized.replace("<strong>Note:</code>", "<strong>Note:</strong>")
                             .replace("<strong>Notes:</code>", "<strong>Note:</strong>")
                             .replace("<strong>Notes:</strong>", "<strong>Note:</strong>");

        return sanitized;
    }

    private static String wrapNotesHtml(String html) {
        if (StringUtils.isBlank(html)) {
            return StringUtils.EMPTY;
        }
        if (DOC_NOTE_HTML_PATTERN.matcher(html).find()) {
            return html;
        }

        var matcher = NOTE_PARAGRAPH_PATTERN.matcher(html);
        var index = 0;
        var result = new StringBuilder();
        while (matcher.find(index)) {
            var start = matcher.start();
            result.append(html, index, start);

            var inner = matcher.group(1);
            var headerOnly = isBlankHtmlContent(inner);
            var noteEnd = matcher.end();

            var cursor = noteEnd;
            var allowParagraphs = headerOnly;
            while (true) {
                cursor = skipWhitespace(html, cursor);
                var blockMatcher = NOTE_FOLLOWING_BLOCK_PATTERN.matcher(html);
                blockMatcher.region(cursor, html.length());
                if (!blockMatcher.lookingAt()) {
                    break;
                }
                var block = blockMatcher.group(1);
                if (!isNoteContinuation(block, allowParagraphs)) {
                    break;
                }
                noteEnd = blockMatcher.end();
                cursor = noteEnd;
            }

            var noteContent = html.substring(start, noteEnd).strip();
            result.append("<blockquote class=\"doc-note\">").append(noteContent).append("</blockquote>");
            index = noteEnd;
        }
        result.append(html.substring(index));
        return result.toString();
    }

    private static boolean isBlankHtmlContent(String value) {
        if (value == null) {
            return true;
        }
        var text = TAG_PATTERN.matcher(value).replaceAll(StringUtils.EMPTY);
        return StringUtils.isBlank(text);
    }

    private static int skipWhitespace(String text, int index) {
        var length = text.length();
        while (index < length) {
            var c = text.charAt(index);
            if (!Character.isWhitespace(c)) {
                break;
            }
            index++;
        }
        return index;
    }

    private static boolean isNoteContinuation(String block, boolean allowParagraphs) {
        if (StringUtils.isBlank(block)) {
            return false;
        }
        var trimmed = block.trim();
        if (!NOTE_CONTINUATION_BLOCK_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        var lower = trimmed.toLowerCase();
        if (lower.startsWith("<p><strong>")) {
            return false;
        }
        if (!allowParagraphs && lower.startsWith("<p")) {
            return false;
        }
        return true;
    }

    private static String replaceSpanBlocks(String source, String spanClass, String replacementTag) {
        if (StringUtils.isBlank(source)) {
            return source;
        }

        var regex = "<span\\s+class=(?:\\\\\\\"|\\\"|')?" + Pattern.quote(spanClass)
                + "(?:\\\\\\\"|\\\"|')?>(.*?)</span>";
        var pattern = Pattern.compile(regex, Pattern.DOTALL);
        var matcher = pattern.matcher(source);
        var buffer = new StringBuffer();
        var found = false;
        while (matcher.find()) {
            var content = matcher.group(1);
            var replacement = "<" + replacementTag + ">"
                    + Matcher.quoteReplacement(content)
                    + "</" + replacementTag + ">";
            matcher.appendReplacement(buffer, replacement);
            found = true;
        }
        matcher.appendTail(buffer);
        if (found) {
            return buffer.toString();
        }
        return source;
    }

    private static String inlineHtmlToMarkdown(String content) {
        if (StringUtils.isBlank(content)) {
            return StringUtils.EMPTY;
        }

        var text = applyInlineMarkdown(content);
        text = TAG_PATTERN.matcher(text).replaceAll(StringUtils.EMPTY);
        text = Strings.CS.replace(text, "&nbsp;", " ");
        text = Strings.CS.replace(text, "&lt;", "<");
        text = Strings.CS.replace(text, "&gt;", ">");
        text = Strings.CS.replace(text, "&amp;", "&");
        return text.replaceAll("\\s+", " ").trim();
    }

    private static String htmlBlockToMarkdown(String html) {
        return htmlBlockToMarkdown(html, true);
    }

    private static String htmlBlockToMarkdown(String html, boolean processNotes) {
        if (StringUtils.isBlank(html)) {
            return StringUtils.EMPTY;
        }

        var text = applyInlineMarkdown(html);
        if (processNotes) {
            text = convertDocNotesToMarkdown(text);
        }
        text = text.replace("<br/>", "  \n");
        text = text.replace("<p>", "").replace("</p>", "\n\n");
        text = text.replace("<ul>", "\n").replace("</ul>", "\n");
        text = text.replace("<ol>", "\n").replace("</ol>", "\n");
        text = text.replace("<li>", "- ").replace("</li>", "\n");
        text = TAG_PATTERN.matcher(text).replaceAll(StringUtils.EMPTY);
        text = Strings.CS.replace(text, "&nbsp;", " ");
        text = Strings.CS.replace(text, "&lt;", "<");
        text = Strings.CS.replace(text, "&gt;", ">");
        text = Strings.CS.replace(text, "&amp;", "&");
        text = text.replaceAll("\n{3,}", "\n\n");
        text = text.replaceAll("[ 	]+\n", "\n");
        return text.trim();
    }

    private static String convertDocNotesToMarkdown(String text) {
        var matcher = DOC_NOTE_HTML_PATTERN.matcher(text);
        var buffer = new StringBuffer();
        while (matcher.find()) {
            var innerHtml = matcher.group(1);
            var innerMarkdown = htmlBlockToMarkdown(innerHtml, false);
            var formatted = formatNoteMarkdown(innerMarkdown);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(formatted));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String formatNoteMarkdown(String markdown) {
        if (StringUtils.isBlank(markdown)) {
            return "**Note:**";
        }
        var normalised = markdown.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (!normalised.startsWith("**Note:**")) {
            normalised = "**Note:** " + normalised;
        }
        var firstBreak = normalised.indexOf('\n');
        if (firstBreak < 0) {
            return normalised;
        }
        var header = normalised.substring(0, firstBreak).trim();
        var body = normalised.substring(firstBreak + 1).trim();
        if (StringUtils.isBlank(body)) {
            return header;
        }
        var formattedBody = body.replace("\n", System.lineSeparator());
        return header + System.lineSeparator() + System.lineSeparator() + formattedBody;
    }

    private static String applyInlineMarkdown(String text) {
        var result = text;
        result = replaceInlineTag(result, Pattern.compile("(?i)<code>(.*?)</code>", Pattern.DOTALL), matcher -> {
            var inner = matcher.group(1).replace("`", "\\`");
            return "`" + inner + "`";
        });
        result = replaceInlineTag(result, Pattern.compile("(?i)<strong>(.*?)</strong>", Pattern.DOTALL), matcher -> "**" + matcher.group(1) + "**");
        result = replaceInlineTag(result, Pattern.compile("(?i)<b>(.*?)</b>", Pattern.DOTALL), matcher -> "**" + matcher.group(1) + "**");
        result = replaceInlineTag(result, Pattern.compile("(?i)<em>(.*?)</em>", Pattern.DOTALL), matcher -> "*" + matcher.group(1) + "*");
        result = replaceInlineTag(result, Pattern.compile("(?i)<i>(.*?)</i>", Pattern.DOTALL), matcher -> "*" + matcher.group(1) + "*");
        return result;
    }

    private static String replaceInlineTag(String text, Pattern pattern, Function<Matcher, String> replacement) {
        var matcher = pattern.matcher(text);
        var buffer = new StringBuffer();
        while (matcher.find()) {
            var replacementText = Matcher.quoteReplacement(replacement.apply(matcher));
            matcher.appendReplacement(buffer, replacementText);
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static final class LongFormat {
        final String html;
        final String markdown;

        LongFormat(String html, String markdown) {
            this.html = html;
            this.markdown = markdown;
        }
    }
    public static String buildHtmlDocument(String bodyContent) {
        return buildHtmlDocument(bodyContent, true);
    }

    public static String buildHtmlDocument(String bodyContent, boolean includeInterFont) {
        var styles = new StringBuilder();
        if (includeInterFont) {
            styles.append("@import url('https://fonts.googleapis.com/css2?family=Inter:ital,opsz,wght@0,14..32,100..900;1,14..32,100..900&display=swap');\n");
        }
        styles.append(COMMON_STYLE);

        var html = new StringBuilder();
        html.append("<html><head>");
        html.append("<script type=\"text/javascript\">")
            .append(CONTEXT_MENU_SCRIPT)
            .append("</script>");
        html.append("<style type=\"text/css\">")
            .append(styles)
            .append("</style>");
        html.append("</head><body>");
        html.append(StringUtils.defaultString(bodyContent));
        html.append("</body></html>");
        return html.toString();
    }

    public static String buildEmptyHtmlDocument() {
        return buildHtmlDocument(StringUtils.EMPTY, false);
    }
}
