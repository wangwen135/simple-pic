package com.wwh.simplepic.model;

import lombok.Data;

/**
 * 水印配置模型
 */
@Data
public class WatermarkConfig {

    private boolean enabled;
    private String type;        // "text" or "image"
    private String content;     // 水印文本内容或图片路径
    private String position;    // top-left, top-right, bottom-left, bottom-right, center
    private double opacity;     // 0.0 ~ 1.0

    public WatermarkConfig() {
        this.enabled = false;
        this.type = "text";
        this.content = "";
        this.position = "bottom-right";
        this.opacity = 0.5;
    }

    /**
     * 生成缓存键的哈希部分，用于判断水印配置是否变更
     */
    public String toHashString() {
        return enabled + "|" + type + "|" + content + "|" + position + "|" + opacity;
    }
}
