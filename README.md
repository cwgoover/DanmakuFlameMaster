DanmakuFlameMaster
==================

### Features

- 使用多种方式(View/SurfaceView/TextureView)实现高效绘制

- B站xml弹幕格式解析

- 基础弹幕精确还原绘制

- 支持mode7特殊弹幕

- 多核机型优化，高效的预缓存机制

- 支持多种显示效果选项实时切换

- 实时弹幕显示支持

- 换行弹幕支持/运动弹幕支持

- 支持自定义字体

- 支持多种弹幕参数设置

- 支持多种方式的弹幕屏蔽

### TODO:

- 增加OpenGL ES绘制方式


### Download
Download the [latest version][1] or grab via Maven:

```xml
<dependency>
  <groupId>com.github.ctiao</groupId>
  <artifactId>dfm</artifactId>
  <version>0.8.3</version>
</dependency>
```

or Gradle:
```groovy
repositories {
    jcenter()
}

dependencies {
    compile 'com.github.ctiao:DanmakuFlameMaster:0.8.3'
    compile 'com.github.ctiao:ndkbitmap-armv7a:0.8.3'

    # Other ABIs: optional
    compile 'com.github.ctiao:ndkbitmap-armv5:0.8.3'
    compile 'com.github.ctiao:ndkbitmap-x86:0.8.3'
}
```
Snapshots of the development version are available in [Sonatype's snapshots repository][2].


[1]:https://oss.sonatype.org/#nexus-search;gav~com.github.ctiao~dfm~~~
[2]:https://oss.sonatype.org/content/repositories/snapshots/
