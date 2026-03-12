# Simple-Pic API 接口文档

## 基本信息

- **基础URL**: `http://localhost:8080`
- **认证方式**: Cookie Token 或 Authorization Header
- **返回格式**: JSON

---

## 公开接口

### 健康检查

```http
GET /api/health
```

**响应示例**:
```json
{
  "status": "ok",
  "service": "Simple-Pic"
}
```

---

## 认证接口

### 登录

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "your_password",
  "rememberMe": true
}
```

**响应示例**:
```json
{
  "success": true,
  "token": "1e952fffa9a84221a920d7e237df94721773207863981",
  "user": {
    "username": "admin",
    "password": "$2a$10$...",
    "role": "ADMIN",
    "storageSpaces": ["default"],
    "currentStorageSpace": "default"
  }
}
```

### 登出

```http
POST /api/auth/logout
Cookie: token=your_token
```

**响应示例**:
```json
{
  "success": true
}
```

### 获取当前用户

```http
GET /api/auth/me
Cookie: token=your_token
```

**响应示例**:
```json
{
  "success": true,
  "user": {
    "username": "admin",
    "role": "ADMIN",
    "storageSpaces": ["default"],
    "currentStorageSpace": "default"
  }
}
```

### 切换存储空间

```http
POST /api/auth/switch-space
Cookie: token=your_token
Content-Type: application/json

{
  "storageSpace": "default"
}
```

**响应示例**:
```json
{
  "success": true,
  "storageSpace": "default"
}
```

---

## 图片接口

### 上传图片

```http
POST /api/image/upload
Cookie: token=your_token
Content-Type: multipart/form-data

file=@image.jpg
```

**响应示例**:
```json
{
  "success": true,
  "message": "Upload successful",
  "url": "http://localhost:8080/api/image/default/2026/03/uuid.jpg",
  "thumbnailUrl": "http://localhost:8080/api/image/thumb/default/2026/03/uuid.jpg",
  "markdown": "![uuid.jpg](http://localhost:8080/api/image/default/2026/03/uuid.jpg)",
  "html": "<img src=\"http://localhost:8080/api/image/default/2026/03/uuid.jpg\" alt=\"uuid.jpg\" />",
  "bbcode": "[img]http://localhost:8080/api/image/default/2026/03/uuid.jpg[/img]",
  "directLink": "http://localhost:8080/api/image/default/2026/03/uuid.jpg",
  "path": "2026/03/uuid.jpg",
  "storageSpace": "default"
}
```

### 获取图片列表

```http
GET /api/image/list?storageSpace=default&path=2026/03&recursive=false
Cookie: token=your_token
```

**参数**:
- `storageSpace`: 存储空间名称（可选，默认从会话获取）
- `path`: 目录路径（可选，默认根目录）
- `recursive`: 是否递归（可选，默认false）

**响应示例**:
```json
{
  "success": true,
  "images": [
    {
      "path": "2026/03/uuid.jpg",
      "name": "uuid.jpg",
      "size": 123456,
      "lastModified": 1705000000000,
      "storageSpace": "default",
      "hasThumbnail": true
    }
  ],
  "directories": ["2026/02", "2026/01"],
  "path": "2026/03",
  "storageSpace": "default"
}
```

### 获取图片

```http
GET /api/image/{storageSpace}/{path}
```

### 获取缩略图

```http
GET /api/image/thumb/{storageSpace}/{path}
```

**说明**: 如果缩略图不存在，返回原图。

### 删除图片

```http
DELETE /api/image/{storageSpace}/{path}
```

**响应示例**:
```json
{
  "success": true
}
```

---

## 管理接口（需要管理员权限）

### 仪表盘数据

```http
GET /api/admin/dashboard
Cookie: token=admin_token
```

**响应示例**:
```json
{
  "storageStats": [...],
  "totalImages": 100,
  "totalUsed": 1073741824,
  "totalSpace": 10737418240,
  "activeSessions": 3,
  "systemName": "Simple-Pic",
  "systemDescription": "简单好用的本地图床"
}
```

### 存储空间管理

**获取所有存储空间**:
```http
GET /api/admin/storages
```

**创建存储空间**:
```http
POST /api/admin/storages
Content-Type: application/json

{
  "name": "custom",
  "path": "./storage/custom",
  "maxSize": "50GB",
  "domain": "http://localhost:8080"
}
```

**更新存储空间**:
```http
PUT /api/admin/storages/{name}
Content-Type: application/json

{
  "path": "./storage/custom",
  "maxSize": "100GB",
  "domain": "http://localhost:8080"
}
```

**删除存储空间**:
```http
DELETE /api/admin/storages/{name}
```

### 用户管理

**获取所有用户**:
```http
GET /api/admin/users
```

**创建用户**:
```http
POST /api/admin/users
Content-Type: application/json

{
  "username": "newuser",
  "password": "password123",
  "role": "USER",
  "storageSpaces": ["default"]
}
```

**更新用户**:
```http
PUT /api/admin/users/{username}
Content-Type: application/json

{
  "password": "newpassword",
  "role": "USER",
  "storageSpaces": ["default"]
}
```

**删除用户**:
```http
DELETE /api/admin/users/{username}
```

### 系统配置

**获取系统配置**:
```http
GET /api/admin/config
```

**更新系统配置**:
```http
PUT /api/admin/config
Content-Type: application/json

{
  "name": "My Image Host",
  "description": "My Description",
  "theme": "dark",
  "itemsPerPage": 50,
  "watermarkEnabled": true,
  "watermarkType": "text",
  "watermarkContent": "My Watermark",
  "watermarkPosition": "bottom-right",
  "watermarkOpacity": 0.5,
  "rateLimitEnabled": true,
  "maxRequests": 100,
  "timeWindow": 60
}
```

### API Key 管理

**获取所有 API Keys**:
```http
GET /api/admin/apikeys
```

**生成 API Key**:
```http
POST /api/admin/apikeys
Content-Type: application/json

{
  "storageSpace": "default"
}
```

**删除 API Key**:
```http
DELETE /api/admin/apikeys/{index}
```

### 后台上传

```http
POST /api/admin/upload
Cookie: token=admin_token
Content-Type: multipart/form-data

file=@image.jpg
storageSpace=default
```

### 创建目录

```http
POST /api/admin/directory
Cookie: token=admin_token
Content-Type: application/json

{
  "path": "2026/04",
  "storageSpace": "default"
}
```

### 删除目录

```http
DELETE /api/admin/directory
Cookie: token=admin_token
Content-Type: application/json

{
  "path": "2026/04",
  "storageSpace": "default"
}
```

### 获取活跃会话

```http
GET /api/admin/sessions
Cookie: token=admin_token
```

---

## 公开 API 上传接口

### API Token 上传

```http
POST /api/upload
Authorization: Bearer your_api_token
Content-Type: multipart/form-data

file=@image.jpg
```

或者使用查询参数：

```http
POST /api/upload
Content-Type: multipart/form-data

token=your_api_token
file=@image.jpg
```

**响应示例**:
```json
{
  "success": true,
  "url": "http://localhost:8080/api/image/default/2026/03/uuid.jpg",
  "path": "2026/03/uuid.jpg",
  "storageSpace": "default"
}
```

---

## 支持的图片格式

- JPEG (.jpg, .jpeg)
- PNG (.png)
- GIF (.gif)
- WebP (.webp)
- SVG (.svg)

---

## 错误响应

所有错误响应格式如下：

```json
{
  "success": false,
  "error": "错误描述信息"
}
```

常见 HTTP 状态码：
- `200`: 成功
- `400`: 请求参数错误
- `401`: 未认证或认证失败
- `403`: 权限不足
- `429`: 请求过于频繁（限流）
- `500`: 服务器内部错误

---

## 限流规则

- 默认限制：每60秒最多100个请求
- 超过限制将返回 429 状态码
- 管理员可在系统设置中调整限流参数

---

## 示例代码

### cURL 上传图片

```bash
curl -X POST http://localhost:8080/api/upload \
  -H "Authorization: Bearer your_api_token" \
  -F "file=@/path/to/image.jpg"
```

### JavaScript 上传图片

```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch('http://localhost:8080/api/image/upload', {
  method: 'POST',
  credentials: 'include',
  body: formData
})
.then(response => response.json())
.then(data => {
  if (data.success) {
    console.log('图片URL:', data.url);
    console.log('Markdown:', data.markdown);
  }
});
```

### Python 上传图片

```python
import requests

url = 'http://localhost:8080/api/upload'
files = {'file': open('image.jpg', 'rb')}
headers = {'Authorization': 'Bearer your_api_token'}

response = requests.post(url, files=files, headers=headers)
data = response.json()

if data['success']:
    print(f"图片URL: {data['url']}")
```

---

## 前端页面

| 页面 | 路径 | 说明 |
|------|------|------|
| 登录页 | /login.html | 用户登录 |
| 上传页 | /upload.html | 图片上传 |
| 管理仪表盘 | /admin/dashboard.html | 管理员后台首页 |
| 图片管理 | /admin/images.html | 图片管理 |
| 存储配置 | /admin/storage.html | 存储空间配置 |
| 用户管理 | /admin/users.html | 用户管理 |
| 系统设置 | /admin/system.html | 系统配置 |
| API密钥 | /admin/apikey.html | API密钥管理 |