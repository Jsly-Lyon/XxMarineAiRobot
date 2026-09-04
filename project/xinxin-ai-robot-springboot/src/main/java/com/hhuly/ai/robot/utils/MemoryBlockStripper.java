package com.hhuly.ai.robot.utils;

import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流式回复中的记忆块剥离工具
 *
 * @author: li
 * @date: 2026/9/2
 * @description: 主对话模型按约定在回复开头输出结构化记忆块（[MEMORY_START]...json...[MEMORY_END]）。
 * 由于流式 chunk 可能把标记切成碎片（如 "[M"、"EMORY"），本工具使用"前缀观望"状态机：
 * 在正文开始前先缓冲少量文本，确认是记忆块则整块吞掉并抽出 JSON，否则按正文透传。
 * 提供两种入口：FrameStripper（逐帧喂入，适配 chatResponse 流按帧解析）与 strip（Flux 整体处理）。
 **/
public final class MemoryBlockStripper {

    /** 记忆块开始标记 */
    public static final String MEMORY_START = "[MEMORY_START]";
    /** 记忆块结束标记 */
    public static final String MEMORY_END = "[MEMORY_END]";

    /** 状态：0=头部判定 1=吸收记忆块 2=正文 */
    private static final int PHASE_HEAD = 0;
    private static final int PHASE_MEMORY = 1;
    private static final int PHASE_BODY = 2;

    private MemoryBlockStripper() {
    }

    /**
     * 单帧正文剥离状态机：维护跨帧状态，每次喂入一个正文文本帧，
     * 返回应透传的正文帧列表（吸收记忆块期间可能返回空列表）。
     *
     * <p>适用于把"推理增量 + 正文"从同一 chatResponse 流解析出来的场景：
     * 每帧只把正文部分交给本状态机，记忆 JSON 会被抽出写入 outRef。</p>
     */
    public static final class FrameStripper {
        private int phase = PHASE_HEAD;
        private final StringBuilder pending = new StringBuilder();  // 头部判定缓冲（确认是否记忆块）
        private final StringBuilder memoryBuf = new StringBuilder(); // 记忆 JSON 内容缓冲
        private final AtomicReference<String> outRef;

        public FrameStripper(AtomicReference<String> outRef) {
            this.outRef = outRef;
        }

        public List<String> feed(String text) {
            List<String> out = new ArrayList<>(2);
            if (text == null || text.isEmpty()) {
                return out;
            }
            switch (phase) {
                case PHASE_BODY -> {
                    // 已进入正文：直接透传
                    out.add(text);
                }
                case PHASE_MEMORY -> {
                    // 正在吸收记忆块
                    memoryBuf.append(text);
                    String after = finishMemoryIfEnded(memoryBuf, outRef);
                    if (after != null) {
                        phase = PHASE_BODY;
                        if (!after.isEmpty()) {
                            out.add(after); // 同一 chunk 中 END 之后的正文
                        }
                    }
                }
                default -> {
                    // 头部判定：先缓冲，按前缀观望是否记忆块开头
                    pending.append(text);
                    String p = pending.toString();

                    if (p.length() >= MEMORY_START.length()) {
                        if (p.startsWith(MEMORY_START)) {
                            // 确认是记忆块：START 之后的内容转入吸收
                            pending.setLength(0);
                            phase = PHASE_MEMORY;
                            String rest = p.substring(MEMORY_START.length());
                            if (!rest.isEmpty()) {
                                memoryBuf.append(rest);
                                String after = finishMemoryIfEnded(memoryBuf, outRef);
                                if (after != null) {
                                    phase = PHASE_BODY;
                                    if (!after.isEmpty()) {
                                        out.add(after);
                                    }
                                }
                            }
                            return out;
                        }
                        // 长度足够且不是记忆块：整段正文
                        pending.setLength(0);
                        phase = PHASE_BODY;
                        if (!p.isEmpty()) {
                            out.add(p);
                        }
                        return out;
                    }

                    if (MEMORY_START.startsWith(p)) {
                        // 仍是 START 的前缀（碎片），观望等待下一块
                        return out;
                    }
                    // 前缀与 START 不一致：非记忆正文
                    pending.setLength(0);
                    phase = PHASE_BODY;
                    if (!p.isEmpty()) {
                        out.add(p);
                    }
                }
            }
            return out;
        }
    }

    /**
     * 对流式文本做剥离：吞掉开头的记忆块，正文透传，记忆 JSON 写入 outRef
     *
     * @param source 原始流
     * @param outRef 接收被剥离记忆 JSON 的容器（无记忆时为空）
     * @return 只含正文的流
     */
    public static Flux<String> strip(Flux<String> source, AtomicReference<String> outRef) {
        return Flux.defer(() -> {
            FrameStripper stripper = new FrameStripper(outRef);
            return source.handle((text, sink) -> {
                for (String body : stripper.feed(text)) {
                    sink.next(body);
                }
            });
        });
    }

    /**
     * 从整段文本中去掉记忆块（含标记），用于消息落库前清洗
     *
     * @param text 原始文本
     * @return 去掉记忆块后的正文
     */
    public static String removeMemoryBlock(String text) {
        if (text == null) {
            return "";
        }
        int start = text.indexOf(MEMORY_START);
        if (start < 0) {
            return text;
        }
        int end = text.indexOf(MEMORY_END, start + MEMORY_START.length());
        if (end < 0) {
            return text;
        }
        return text.substring(0, start) + text.substring(end + MEMORY_END.length());
    }

    /**
     * 若 memoryBuf 已含结束标记，则取出记忆 JSON 写入 outRef，并返回结束标记之后的剩余正文；否则返回 null
     */
    private static String finishMemoryIfEnded(StringBuilder memoryBuf, AtomicReference<String> outRef) {
        int endIdx = memoryBuf.indexOf(MEMORY_END);
        if (endIdx < 0) {
            return null;
        }
        outRef.set(memoryBuf.substring(0, endIdx));
        String after = memoryBuf.substring(endIdx + MEMORY_END.length());
        memoryBuf.setLength(0);
        return after;
    }
}
