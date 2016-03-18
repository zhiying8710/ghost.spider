package me.binge.ghost.spiders.entity;

import org.apache.commons.lang3.StringUtils;

public class Article {

    public static final String CONTENT_SEP = "\001\001\001";

    private String url;
    private String postUrl;
    private String subject;
    private String content;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPostUrl() {
        return postUrl;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }

    @Override
    public String toString() {
        return "Article [url=" + url + ", subject="
                + subject + ", content=" + (StringUtils.isBlank(content) ? 0 : content.length()) + "]";
    }

}
