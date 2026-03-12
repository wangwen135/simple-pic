# Simple-Pic 本地图床应用

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-1.8+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-green.svg)](https://spring.io/projects/spring-boot)

Simple-Pic 是一个简单好用的本地图片托管应用，支持多用户、多存储空间、后台管理、API上传、缩略图生成、水印等功能，无需数据库依赖。

---

## 功能特性

- 🚀 **多用户支持** - 管理员和普通用户角色，BCrypt密码加密
- 📁 **多存储空间** - 支持配置多个存储目录，独立空间配额管理
- 📤 **便捷上传** - 支持文件选择、拖拽上传、剪贴板粘贴三种方式
- 🔗 **多种链接** - Markdown、直链、HTML、BBCode 四种格式一键复制
- 🎨 **水印功能** - 支持文字水印，自定义位置和透明度
- 🖼️ **缩略图生成** - 自动生成缩略图，支持定时批量生成
- 🔐 **API接口** - Token鉴权的API上传接口
- 📊 **后台管理** - 完整的图片、用户、存储空间管理系统
- 🛡️ **安全防护** - IP限流、会话管理、密码加密
- 💾 **零数据库** - 所有配置存储在YAML文件中

---

## 快速开始

### 环境要求

- Java 1.8 或更高版本
- Maven 3.6+

### 安装部署

1. 克隆或下载项目

```bash
git clone <repository-url>
cd simple-pic
```

2. 编译打包

```bash
mvn clean package
```

3. 运行应用

```bash
java -jar target/simple-pic-1.0.0.jar
```

### 首次登录

首次启动时，应用程序会：
1. 在项目根目录生成 `config.yml` 配置文件
2. 为管理员账户生成随机密码，并输出到控制台

**示例输出：**
```
==============================================
ADMIN CREDENTIALS:
Username: admin
Password: AbCdEf1234!@#
==============================================
```

使用这些凭据登录：http://localhost:8080/login.html

### 访问地址

- **上传页面**: http://localhost:8080/upload.html
- **登录页面**: http://localhost:8080/login.html
- **管理后台**: http://localhost:8080/admin/dashboard.html
- **API健康检查**: http://localhost:8080/api/health

---

## 使用说明

### 上传图片

1. 登录应用
2. 进入上传页面
3. 选择以下任一方式上传：
   - 点击上传区域选择文件
   - 拖拽图片到上传区域
   - 按 Ctrl+V 粘贴剪贴板图片
4. 上传成功后，链接会自动复制到剪贴板

### 管理后台

登录后台可进行以下操作：

| 模块 | 功能 |
|------|------|
| 仪表盘 | 查看存储统计和系统信息 |
| 图片管理 | 浏览、上传、删除图片，管理目录 |
| 存储配置 | 创建、编辑、删除存储空间 |
| 用户管理 | 创建、编辑、删除用户 |
| 系统设置 | 配置水印、安全、前端等 |
| API密钥 | 生成和管理API上传密钥 |

### API上传

```bash
curl -X POST http://localhost:8080/api/upload \
  -H "Authorization: Bearer YOUR_API_TOKEN" \
  -F "file=@image.jpg"
```

详细API文档请查看：[API接口文档](doc/03-API接口文档.md)

---

## 配置说明

配置文件 `config.yml` 位于项目根目录：

```yaml
simple-pic:
  system:
    name: Simple-Pic
    description: 简单好用的本地图床

  storage-spaces:
    - name: default
      path: ./storage/default
      max-size: 10GB
      domain: http://localhost:8080

  users:
    - username: admin
      password: {BCRYPT加密密码}
      role: ADMIN
      storage-spaces: [default]

  api-keys:
    - token: YOUR_TOKEN
      storage-space: default

  watermark:
    enabled: true
    type: text
    content: Simple-Pic
    position: bottom-right
    opacity: 0.5

  security:
    rate-limit:
      enabled: true
      max-requests: 100
      time-window: 60

  frontend:
    theme: dark
    items-per-page: 50
```

---

## 项目结构

```
simple-pic/
├── src/main/
│   ├── java/com/simplepic/
│   │   ├── SimplePicApplication.java    # 启动类
│   │   ├── config/                       # 配置类
│   │   ├── controller/                   # 控制器
│   │   ├── service/                      # 服务层
│   │   ├── model/                        # 数据模型
│   │   ├── security/                     # 安全组件
│   │   └── interceptor/                  # 拦截器
│   └── resources/
│       ├── static/                       # 静态资源
│       │   ├── login.html
│       │   ├── upload.html
│       │   └── admin/                    # 管理后台页面
│       └── application.yml              # Spring配置
├── doc/                                  # 文档目录
├── config.yml                            # 应用配置
├── pom.xml                               # Maven配置
└── README.md                             # 项目说明
```

---

## 支持的图片格式

- JPEG (.jpg, .jpeg)
- PNG (.png)
- GIF (.gif)
- WebP (.webp)
- SVG (.svg)

---

## 日志说明

日志文件位于 `logs/simple-pic.log`，具有以下特性：

- 单文件最大 100MB
- 自动滚动归档
- 保留最近 30 天日志
- 使用 GZIP 压缩

---

## 常见问题

**Q: 忘记管理员密码？**

A: 删除 `config.yml` 文件并重启应用，系统会重新生成随机密码。

**Q: 如何修改存储空间大小？**

A: 登录管理后台，进入"存储"页面，点击对应存储空间的编辑按钮。

**Q: 如何生成API密钥？**

A: 登录管理后台，进入"API Keys"页面，点击"生成API Key"按钮。

**Q: 上传失败提示配额已满？**

A: 在管理后台查看存储空间使用情况，可以增加配额或清理旧图片。

---

## 技术栈

- **后端框架**: Spring Boot 2.7.18
- **Java版本**: 1.8+
- **图片处理**: Thumbnailator
- **密码加密**: BCrypt
- **限流**: Guava RateLimiter
- **配置管理**: SnakeYAML
- **前端框架**: 纯HTML + JavaScript + Tailwind CSS

---

## 依赖项

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- SnakeYAML -->
    <dependency>
        <groupId>org.yaml</groupId>
        <artifactId>snakeyaml</artifactId>
    </dependency>

    <!-- Spring Security Crypto -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-crypto</artifactId>
    </dependency>

    <!-- Thumbnailator -->
    <dependency>
        <groupId>net.coobird</groupId>
        <artifactId>thumbnailator</artifactId>
        <version>0.4.19</version>
    </dependency>

    <!-- Guava -->
    <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
        <version>31.1-jre</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 许可证

MIT License

---

## 文档

- [设计文档](doc/01-设计文档.md)
- [API接口文档](doc/02-API接口文档.md)
- [用户使用手册](doc/03-用户使用手册.md)

