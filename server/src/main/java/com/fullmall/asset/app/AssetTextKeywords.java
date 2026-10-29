package com.fullmall.asset.app;

import java.util.ArrayList;
import java.util.List;

/**
 * 素材标题/描述 → 检索标签（确定性规则，不调用模型）。
 *
 * <p>为什么不用模型抽标签：标签要稳定、可复现、零成本；模型输出只用作描述，
 * 标签由本地规则从文本里抽，避免"同一个素材每次生成不同标签"。
 */
final class AssetTextKeywords {

    private static final int MAX_TAGS = 8;
    private static final List<String> STOP_WORDS = List.of(
            "一个", "一张", "请", "生成", "图片", "视频", "the", "and", "with", "this", "that");

    private AssetTextKeywords() {
    }

    static List<String> of(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        // 中文没有空格：优先按标点切；英文按空白切。片段过短/过长都丢弃。
        for (String raw : text.split("[\\s,，。.;；、/|（）()\\[\\]{}:：!?！？“”\"']+")) {
            String word = raw.trim();
            if (word.length() < 2 || word.length() > 16 || STOP_WORDS.contains(word)) {
                continue;
            }
            if (!result.contains(word)) {
                result.add(word);
            }
            if (result.size() >= MAX_TAGS) {
                break;
            }
        }
        return result;
    }
}
