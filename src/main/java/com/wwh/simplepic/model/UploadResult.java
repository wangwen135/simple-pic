package com.wwh.simplepic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Upload result model
 * 上传结果模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResult {

    private boolean success;
    private String message;
    private String url;
    private String thumbnailUrl;
    private String markdown;
    private String html;
    private String bbcode;
    private String directLink;
    private String path;
    private String storageSpace;

    public static UploadResult success(String url, String thumbnailUrl, String path, String storageSpace) {
        UploadResult result = new UploadResult();
        result.setSuccess(true);
        result.setMessage("Upload successful");
        result.setUrl(url);
        result.setThumbnailUrl(thumbnailUrl);
        result.setPath(path);
        result.setStorageSpace(storageSpace);

        // Generate different link formats
        result.setDirectLink(url);
        result.setMarkdown(String.format("![%s](%s)", path, url));
        result.setHtml(String.format("<img src=\"%s\" alt=\"%s\" />", url, path));
        result.setBbcode(String.format("[img]%s[/img]", url));

        return result;
    }

    public static UploadResult error(String message) {
        UploadResult result = new UploadResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}