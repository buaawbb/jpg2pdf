# JPG 转 PDF

一个 Android 应用，用于将 JPG/PNG 图片无损转换为 PDF 文档，支持裁剪和白框标注功能。
<img width="49%" height="49%" alt="b2c0de664e9a7472a5354c52d0d99a31" src="https://github.com/user-attachments/assets/1b8a5cda-70e8-407d-b61a-34ce98589678" /> <img width="49%" height="49%" alt="6b4cd909201f8755ebef537ee71707b0" src="https://github.com/user-attachments/assets/0fdc2bb0-471b-48b0-81f5-f44cce21da5b" />


## 功能

- **图片转 PDF** — 将多张图片合并转换为一个 PDF 文件，不压缩、不缩放，保持原始画质
- **图片来源** — 支持从相册、截图、下载目录或自定义文件夹选择图片；支持自动加载指定数量的最新图片
- **裁剪** — 交互式裁剪框，支持拖拽调整裁剪区域，去除图片中不需要的部分
- **白框标注** — 在图片上添加白色矩形框，可用于遮盖敏感信息
- **PDF 分享** — 通过系统分享对话框直接分享生成的 PDF 文件
- **删除原图** — 可选：生成 PDF 后自动删除原始图片

## 技术栈

- **语言**: Java
- **最低 SDK**: 26 (Android 8.0)
- **目标 SDK**: 34 (Android 14)
- **构建工具**: Gradle 9.3.1, Android Gradle Plugin 9.1.0
- **UI**: Material Design 3, ConstraintLayout

**主要依赖:**

| 依赖 | 版本 | 用途 |
|---|---|---|
| AppCompat | 1.6.1 | 兼容组件库 |
| Material | 1.11.0 | Material Design 3 组件 |
| ConstraintLayout | 2.1.4 | 布局 |
| Glide | 4.16.0 | 图片加载与缓存 |
| DocumentFile | 1.0.1 | 存储访问框架 (SAF) |

## 构建

### 前置条件

- Android SDK（路径配置在 `local.properties`）
- Java 8+

### 命令

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 安装到连接的设备
./gradlew installDebug

# 清理构建
./gradlew clean
```

也可以在 Android Studio 中打开项目，同步 Gradle 后直接运行。

## 项目结构

```
app/
├── build.gradle              # 模块构建配置
├── proguard-rules.pro        # ProGuard 混淆规则
└── src/main/
    ├── AndroidManifest.xml   # 清单文件
    ├── java/com/jpg2pdf/app/
    │   ├── MainActivity.java        # 主界面与核心逻辑
    │   ├── PdfGenerator.java        # PDF 无损生成引擎
    │   ├── CropOverlayView.java     # 裁剪/白框叠加层控件
    │   ├── PdfShareHelper.java      # PDF 分享辅助
    │   └── ThumbnailAdapter.java    # 缩略图列表适配器
    └── res/
        ├── drawable/                # 图标资源
        ├── layout/                  # 布局文件
        ├── values/                  # 字符串、颜色、主题
        └── xml/file_paths.xml       # FileProvider 路径配置
```

## 核心设计

**PDF 生成** (`PdfGenerator.java`): 使用 Android 原生 `PdfDocument` API 逐页绘制。每张图片解码为 Bitmap 后直接绘制到 PDF 页面，不做缩放或重压缩，确保输出与原图一致。

**裁剪/白框** (`CropOverlayView.java`): 自定义 View 叠加在图片上方，支持触摸拖拽调整矩形区域。裁剪和白框区域以 `RectF` 传递到生成器，在绘制到 PDF 时计算映射。

**权限处理**: 适配不同 Android 版本的存储权限模型——Android 13+ 使用细粒度 `READ_MEDIA_IMAGES`，Android 9 及以下使用 `WRITE_EXTERNAL_STORAGE`，Android 10+ 支持 `MANAGE_EXTERNAL_STORAGE` 用于文件夹级别的访问。

## License

MIT
