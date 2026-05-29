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
    private int fontSize;       // 文字字号
    private String color;       // 文字颜色
    private int margin;         // 边距
    private boolean outlineEnabled; // 是否启用描边
    private String outlineColor;    // 描边颜色
    private boolean shadowEnabled;  // 是否启用阴影
    private boolean tileEnabled;    // 是否平铺
    private int tileGap;            // 平铺间距
    private int angle;              // 旋转角度

    public WatermarkConfig() {
        this.enabled = false;
        this.type = "text";
        this.content = "";
        this.position = "bottom-right";
        this.opacity = 0.5;
        this.fontSize = 20;
        this.color = "#ffffff";
        this.margin = 10;
        this.outlineEnabled = true;
        this.outlineColor = "#000000";
        this.shadowEnabled = true;
        this.tileEnabled = false;
        this.tileGap = 120;
        this.angle = 0;
    }

    /**
     * 生成缓存键的哈希部分，用于判断水印配置是否变更
     */
    public String toHashString() {
        return enabled + "|" + type + "|" + content + "|" + position + "|" + opacity
                + "|" + fontSize + "|" + color + "|" + margin + "|" + outlineEnabled
                + "|" + outlineColor + "|" + shadowEnabled + "|" + tileEnabled + "|" + tileGap + "|" + angle;
    }
}
