# Simple-Pic 使用说明

零数据库的本地图床应用，开箱即用。

## 环境要求

- JDK 8+

## 快速启动

### Linux / macOS

```bash
./simple-pic.sh start
```

### Windows

```bat
simple-pic.bat start
```

首次启动时会自动生成 `config.yml` 并在控制台输出管理员密码：

```
==============================================
ADMIN CREDENTIALS:
Username: admin
Password: AbCdEf1234!@#
==============================================
```

使用该密码登录：http://localhost:8080/login.html

## 管理命令

```bash
# Linux / macOS
./simple-pic.sh start      # 启动
./simple-pic.sh stop       # 停止
./simple-pic.sh restart    # 重启
./simple-pic.sh status     # 查看状态

# Windows
simple-pic.bat start
simple-pic.bat stop
simple-pic.bat restart
simple-pic.bat status
```

## 修改端口

编辑 `application.yml`：

```yaml
server:
  port: 9090    # 改为你需要的端口
```

也可以通过环境变量覆盖（不修改文件）：

```bash
SERVER_PORT=9090 ./simple-pic.sh start
```

修改端口后需要重启生效。

## 配置说明

| 文件 | 用途 |
|------|------|
| `application.yml` | 框架配置：端口、日志、上传限制等 |
| `config.yml` | 业务配置：用户、存储空间、水印、防盗链等（首次启动自动生成） |

业务配置可在后台管理页面直接修改，无需手动编辑。

## API 上传

先在后台「API 密钥管理」创建密钥，然后：

```bash
curl -X POST http://localhost:8080/api/upload \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -F "file=@image.jpg"
```

## 功能列表

- 图片上传：选择文件、拖拽、剪贴板粘贴
- 链接复制：直链、Markdown、HTML、BBCode
- 后台管理：图片、用户、存储空间、API Key、系统设置
- 水印配置：文字、位置、透明度、颜色、描边、阴影、平铺、旋转
- 安全控制：登录认证、API Token、防盗链、IP 限流
