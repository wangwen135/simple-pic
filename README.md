# Simple-Pic

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-green.svg)](https://spring.io/projects/spring-boot)

Simple-Pic 是一个零数据库的本地图床应用，基于 Java 8 和 Spring Boot 2.7。

它支持图片上传、多用户、多存储空间、后台管理、API 上传、水印、防盗链和访问限流。运行数据保存在本地文件和 `config.yml` 中，不依赖数据库。

## 截图

截图放在 `doc/images/` 目录下，文件名可按下面示例保存。

![上传页面](doc/images/upload.png)
![仪表盘](doc/images/dashboard.png)
![图片管理](doc/images/images.png)
![系统设置](doc/images/system.png)

## 功能

- 图片上传：支持选择文件、拖拽、剪贴板粘贴
- 链接复制：支持直链、Markdown、HTML、BBCode
- 后台管理：图片、用户、存储空间、API Key、系统设置
- 水印配置：支持文字水印、位置、透明度、字号、颜色、描边、阴影、平铺、旋转
- 安全控制：登录认证、API Token、防盗链、IP 限流
- 本地配置：无需数据库，配置和运行数据保存在本地

## 快速启动

环境要求：

- Java 8+
- Maven 3.6+

```bash
mvn clean package
java -jar target/simple-pic-1.0.0.jar
```

首次启动会在项目根目录生成 `config.yml`，管理员账号和随机密码会输出到控制台。

常用地址：

- 上传页面：`http://localhost:8080/upload.html`
- 登录页面：`http://localhost:8080/login.html`
- 管理后台：`http://localhost:8080/admin/dashboard.html`
- 健康检查：`http://localhost:8080/api/health`

## API 上传

```bash
curl -X POST http://localhost:8080/api/upload \
  -H "Authorization: Bearer YOUR_API_TOKEN" \
  -F "file=@image.jpg"
```

更多说明见 [API 接口文档](doc/02-API接口文档.md)。

## 配置

主要配置文件为项目根目录下的 `config.yml`。首次启动自动生成，可在后台页面维护用户、存储空间、水印、防盗链、上传限制等设置。

不要提交真实的 `config.yml`、API Token、上传文件或日志。

## 文档

- [设计文档](doc/01-设计文档.md)
- [API 接口文档](doc/02-API接口文档.md)
- [用户使用手册](doc/03-用户使用手册.md)

## 许可证

[Apache License 2.0](LICENSE)
