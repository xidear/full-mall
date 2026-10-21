package com.fullmall.ai.app;

import com.fullmall.platform.error.ApiException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 客户资料导入：解析 → 切分 → 入库。
 *
 * <p>支持的格式与失败语义见 `docs/AI_KNOWLEDGE_IMPORT_GUIDE.md`：
 * 图片/扫描件/空文档都会**明确报错**，不会生成空知识块。
 */
@Service
public class DocumentIngestionService {

    /** 单块字数上限：太长会稀释检索命中，太短会丢上下文。 */
    static final int MAX_CHUNK_CHARS = 500;
    static final int MAX_TEXT_CHARS = 20_000_000;

    private final KnowledgeStore store;

    public DocumentIngestionService(KnowledgeStore store) {
        this.store = store;
    }

    public record ImportResult(String sourceId, String detectedType, int chunks, int chars) {
    }

    /** 与 ingest 等价，命名更贴近 controller 语义。 */
    public ImportResult importFile(String fileName, byte[] content) {
        return ingest(fileName, content);
    }

    public ImportResult ingest(String fileName, byte[] content) {
        if (content == null || content.length == 0) {
            throw ApiException.badRequest("EMPTY_DOCUMENT", "文件内容为空：" + fileName);
        }
        String text = extract(fileName, content);
        if (text.isBlank()) {
            throw ApiException.badRequest("EMPTY_DOCUMENT",
                    "解析后没有文字（可能是扫描版 PDF 或纯图片）：" + fileName);
        }
        if (text.length() > MAX_TEXT_CHARS) {
            throw ApiException.badRequest("FILE_TOO_LARGE",
                    "解析文本超过上限 " + MAX_TEXT_CHARS + " 字：" + fileName);
        }
        String sourceId = UUID.randomUUID().toString();
        List<KnowledgeChunk> chunks = split(fileName, sourceId, text);
        store.replaceSource("DOCUMENT", sourceId, chunks);
        return new ImportResult(sourceId, detectType(fileName), chunks.size(), text.length());
    }

    /** 用 Tika 抽文本；不支持的二进制类型直接拒绝，而不是存一堆乱码。 */
    String extract(String fileName, byte[] content) {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        boolean supported = lower.endsWith(".pdf") || lower.endsWith(".docx") || lower.endsWith(".doc")
                || lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown")
                || lower.endsWith(".csv") || lower.endsWith(".html") || lower.endsWith(".htm")
                || lower.endsWith(".xlsx") || lower.endsWith(".xls");
        if (!supported) {
            throw ApiException.badRequest("UNSUPPORTED_FORMAT",
                    "不支持的文件类型：" + fileName
                            + "（支持 pdf/docx/doc/txt/md/csv/html/xlsx/xls）");
        }
        try (var stream = new ByteArrayInputStream(content)) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            metadata.set(org.apache.tika.metadata.TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            new AutoDetectParser().parse(stream, handler, metadata, new ParseContext());
            return handler.toString().replace("\r\n", "\n").trim();
        } catch (Exception error) {
            throw ApiException.badRequest("PARSE_FAILED",
                    "文档解析失败（" + fileName + "）：" + error.getMessage());
        }
    }

    /** 按段落切块，超长段落按句号二次切分；每块保留来源标题。 */
    List<KnowledgeChunk> split(String fileName, String sourceId, String text) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String title = fileName == null ? "未命名文档" : fileName;

        for (String paragraph : text.split("\n\\s*\n")) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() <= MAX_CHUNK_CHARS) {
                chunks.add(chunk(title, sourceId, trimmed, chunks.size()));
                continue;
            }
            StringBuilder buffer = new StringBuilder();
            for (String sentence : trimmed.split("(?<=[。！？.!?])")) {
                if (buffer.length() + sentence.length() > MAX_CHUNK_CHARS && buffer.length() > 0) {
                    chunks.add(chunk(title, sourceId, buffer.toString().trim(), chunks.size()));
                    buffer.setLength(0);
                }
                buffer.append(sentence);
            }
            if (buffer.length() > 0) {
                chunks.add(chunk(title, sourceId, buffer.toString().trim(), chunks.size()));
            }
        }
        return chunks;
    }

    private KnowledgeChunk chunk(String title, String sourceId, String content, int index) {
        return new KnowledgeChunk(UUID.randomUUID(), "DOCUMENT", sourceId,
                title + " · 第 " + (index + 1) + " 段", content, "v1");
    }

    private String detectType(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot < 0 ? "unknown" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
