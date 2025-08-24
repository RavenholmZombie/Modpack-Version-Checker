package com.mesabrook.rzcraft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public final class VersionCheck {
    private static final Logger LOG = LogManager.getLogger("PackGate/VersionCheck");

    public enum State { RUNNING, OK, OUTDATED, ERROR }

    public static final class Result {
        public final State state;
        public final String current;
        public final String latest;      // null on ERROR
        public final String downloadUrl; // effective button URL (never null)
        public final String configDownloadUrl;
        public Result(State s, String cur, String lat, String dl, String cfg) {
            this.state = s; this.current = cur; this.latest = lat; this.downloadUrl = dl; this.configDownloadUrl = cfg;
        }
        @Override public String toString() {
            return "Result{state=" + state + ", current=" + current + ", latest=" + latest + ", button=" + downloadUrl + "}";
        }
    }

    private static final Pattern STRICT_VERSION = Pattern.compile("^\\d+(?:\\.\\d+)*$");
    private static volatile CompletableFuture<Result> future = null;
    private VersionCheck() {}

    public static synchronized CompletableFuture<Result> start(
            String remoteUrl, String configDownloadUrl, String currentVersion, int timeoutSeconds) {

        if (future != null) {
            LOG.debug("start(): already running/finished -> {}", future);
            return future;
        }

        final String name = safeTrim(Config.MODPACK_NAME.get());
        final String pack = (name == null || name.isEmpty()) ? "your modpack" : name;

        LOG.info("[{}] Version check starting…", pack);
        LOG.info("  remoteURL      = {}", safeTrim(remoteUrl));
        LOG.info("  downloadURL(cfg)= {}", safeTrim(configDownloadUrl));
        LOG.info("  local version  = {}", safeTrim(currentVersion));
        LOG.info("  format         = {}", safeTrim(Config.FORMAT.get()));
        LOG.info("  timeout (s)    = {}", timeoutSeconds);

        // choose a safe default for the button URL
        String effectiveDownload = (!isBlank(configDownloadUrl)) ? configDownloadUrl.trim() : remoteUrl;

        // Validate URL before we build the request
        URI remoteUri = null;
        try {
            if (remoteUrl != null) remoteUri = URI.create(remoteUrl.trim());
            String sch = (remoteUri != null) ? remoteUri.getScheme() : null;
            if (sch == null || !(sch.equalsIgnoreCase("http") || sch.equalsIgnoreCase("https"))) {
                LOG.warn("Invalid or missing scheme in remoteURL: {}", remoteUrl);
                future = CompletableFuture.completedFuture(
                        new Result(State.ERROR, currentVersion, null, effectiveDownload, configDownloadUrl));
                return future;
            }
        } catch (Exception ex) {
            LOG.warn("remoteURL is not a valid URI: {}", remoteUrl, ex);
            future = CompletableFuture.completedFuture(
                    new Result(State.ERROR, currentVersion, null, effectiveDownload, configDownloadUrl));
            return future;
        }

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(remoteUri)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Accept", "application/json,text/plain;q=0.9,*/*;q=0.1")
                .build();

        final String formatPref = safeTrimLower(Config.FORMAT.get(), "auto");
        LOG.info("HTTP GET {}  (Accept: {})", remoteUri, "application/json,text/plain;q=0.9,*/*;q=0.1");

        future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    int code = resp.statusCode();
                    String body = Objects.toString(resp.body(), "").replace("\r", "").trim();
                    String ctype = resp.headers().firstValue("content-type").orElse("");
                    LOG.info("HTTP {}  Content-Type='{}'  bytes={}", code, ctype, body.length());

                    if (code < 200 || code >= 300 || body.isEmpty()) {
                        LOG.warn("Unsuccessful response or empty body; treating as ERROR (allowing play by policy).");
                        return new Result(State.ERROR, currentVersion, null, effectiveDownload, configDownloadUrl);
                    }

                    boolean looksJson = ctype.toLowerCase().contains("json") || body.startsWith("{") || body.startsWith("[");
                    ParsedRemote pr;
                    try {
                        if ("json".equals(formatPref) || ("auto".equals(formatPref) && looksJson)) {
                            LOG.info("Parsing as JSON (pref='{}', looksJson={})", formatPref, looksJson);
                            pr = parseJson(body);
                        } else {
                            LOG.info("Parsing as TEXT  (pref='{}', looksJson={})", formatPref, looksJson);
                            pr = parseText(body);
                        }
                    } catch (Exception parseEx) {
                        LOG.warn("Parse exception; treating as ERROR.", parseEx);
                        return new Result(State.ERROR, currentVersion, null, effectiveDownload, configDownloadUrl);
                    }

                    LOG.info("Parsed latest='{}'  download='{}'", pr.latest, pr.download);

                    if (pr.latest == null || !STRICT_VERSION.matcher(pr.latest).matches()) {
                        LOG.warn("Latest version missing or not numeric dotted format: '{}'", pr.latest);
                        return new Result(State.ERROR, currentVersion, null, effectiveDownload, configDownloadUrl);
                    }

                    String buttonUrl = (!isBlank(configDownloadUrl)) ? configDownloadUrl.trim()
                            : (!isBlank(pr.download)) ? pr.download
                            : remoteUrl;
                    LOG.info("Button URL chosen = {}", buttonUrl);

                    boolean newer = isRemoteNewer(pr.latest, currentVersion);
                    LOG.info("Compare local='{}' vs remote='{}' → {}", currentVersion, pr.latest, newer ? "OUTDATED" : "OK");

                    return newer
                            ? new Result(State.OUTDATED, currentVersion, pr.latest, buttonUrl, configDownloadUrl)
                            : new Result(State.OK,       currentVersion, pr.latest, buttonUrl, configDownloadUrl);
                })
                .exceptionally(ex -> {
                    LOG.error("HTTP exception; treating as ERROR (allow play by policy).", ex);
                    return new Result(State.ERROR, currentVersion, null, effectiveDownload, configDownloadUrl);
                });

        return future;
    }

    public static CompletableFuture<Result> getFuture() { return future; }
    public static boolean isDone() { return future != null && future.isDone(); }
    public static Result getNowOrNull() { return future != null ? future.getNow(null) : null; }

    // ----- helpers -------------------------------------------------------------

    private record ParsedRemote(String latest, String download) {}

    private static ParsedRemote parseJson(String body) {
        JsonElement el = JsonParser.parseString(body);
        JsonObject obj = el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();

        String latest =
                getString(obj, "latest",
                        getString(obj, "version",
                                getString(obj, "modpackVersion", null)));

        String download =
                getString(obj, "downloadURL",
                        getString(obj, "download_url",
                                getString(obj, "url", null)));

        return new ParsedRemote(latest, download);
    }

    private static ParsedRemote parseText(String body) {
        String latest = null, download = null;
        for (String line : body.split("\n")) {
            String s = line.strip();
            if (s.isEmpty()) continue;
            if (latest == null) latest = s;
            else if (s.regionMatches(true, 0, "url=", 0, 4)) download = s.substring(4).trim();
        }
        return new ParsedRemote(latest, download);
    }

    private static String getString(JsonObject o, String key, String dflt) {
        if (o.has(key) && o.get(key).isJsonPrimitive() && o.get(key).getAsJsonPrimitive().isString()) {
            return o.get(key).getAsString().trim();
        }
        return dflt;
    }

    /** dotted-number compare (1.10.0 > 1.9.9) */
    static boolean isRemoteNewer(String remote, String local) {
        String[] r = remote.split("\\.");
        String[] l = local.split("\\.");
        int n = Math.max(r.length, l.length);
        for (int i = 0; i < n; i++) {
            int ri = i < r.length ? parseInt(r[i]) : 0;
            int li = i < l.length ? parseInt(l[i]) : 0;
            if (ri != li) return ri > li;
        }
        return false;
    }
    private static int parseInt(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String safeTrim(String s) { return s == null ? null : s.trim(); }
    private static String safeTrimLower(String s, String dflt) { s = safeTrim(s); return s == null ? dflt : s.toLowerCase(); }
}
