package com.screenshotmaker.storage.manifest;

/**
 * Renders a {@link ManifestDocument} as a single self-contained HTML gallery page, so a run's
 * output folder can be browsed by double-clicking {@code index.html} &mdash; no server required.
 * Pure string transformation: easy to unit test, no I/O.
 */
public final class GalleryHtmlRenderer {

    private GalleryHtmlRenderer() {
    }

    public static String render(ManifestDocument doc) {
        StringBuilder html = new StringBuilder(4096);
        html.append("<!doctype html><html><head><meta charset=\"utf-8\">")
                .append("<title>Screenshot Maker: ").append(escape(doc.targetSource())).append("</title>")
                .append("<style>")
                .append("body{font-family:system-ui,Segoe UI,Arial,sans-serif;background:#111;color:#eee;margin:0;padding:24px}")
                .append("h1{font-size:18px;font-weight:600}")
                .append(".meta{color:#9aa;font-size:13px;margin-bottom:20px}")
                .append(".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px}")
                .append(".card{background:#1b1b1f;border:1px solid #2a2a30;border-radius:8px;overflow:hidden}")
                .append(".card img{width:100%;display:block;border-bottom:1px solid #2a2a30}")
                .append(".card .body{padding:10px 12px}")
                .append(".card .label{font-size:13px;font-weight:600;margin:0 0 4px}")
                .append(".card .ref{font-size:11px;color:#9aa;word-break:break-all}")
                .append(".errors{margin-top:28px}")
                .append(".errors li{color:#e88;font-size:13px}")
                .append("</style></head><body>")
                .append("<h1>").append(escape(doc.targetType())).append(": ").append(escape(doc.targetSource())).append("</h1>")
                .append("<div class=\"meta\">").append(doc.totalScreens()).append(" screen(s) captured, ")
                .append(doc.totalErrors()).append(" error(s) &middot; ")
                .append(escape(String.valueOf(doc.startedAt()))).append(" &rarr; ")
                .append(escape(String.valueOf(doc.finishedAt()))).append("</div>")
                .append("<div class=\"grid\">");

        for (ManifestScreenEntry entry : doc.screens()) {
            html.append("<div class=\"card\">")
                    .append("<img loading=\"lazy\" src=\"").append(escape(entry.fileName())).append("\" alt=\"\">")
                    .append("<div class=\"body\">")
                    .append("<p class=\"label\">#").append(entry.sequence()).append(" &mdash; ").append(escape(entry.label())).append("</p>")
                    .append("<p class=\"ref\">").append(escape(entry.sourceRef())).append("</p>")
                    .append("</div></div>");
        }

        html.append("</div>");

        if (!doc.errors().isEmpty()) {
            html.append("<div class=\"errors\"><h2>Errors</h2><ul>");
            for (ManifestErrorEntry error : doc.errors()) {
                html.append("<li>").append(escape(error.sourceRef())).append(": ").append(escape(error.message())).append("</li>");
            }
            html.append("</ul></div>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
