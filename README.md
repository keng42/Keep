# Keep
仿照 Google Keep 的手机日记软件，主要功能是输入并加密文字和图片，并可根据文件夹、标签和位置进行分类。  
图片加密后存储于 SDCard 中并缓存小尺寸解密后的图片于系统分区，解决两个问题：系统分区容量问题；SDCard 隐私问题。  

# 程序运行截图
### 程序首次运行时需要设置安全码和数据加密密码
安全码用于进入程序时验证和加密数据加密密码；数据加密密码用于加密图片和文字。设备中存储安全码的哈希值用于验证，存储数据加密密码的密文用于加密解密图片和文字。  
加密解密模块使用 [Facebook Conceal](https://facebook.github.io/conceal/) 完成  
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/00SetPassword.png" width = "360" height = "640" alt="Keep"/>

### 进入程序需要验证安全码
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/01Lock.png" width = "360" height = "640" alt="Keep"/>

### 抽屉菜单
可选择按颜色（文件夹）、标签、位置、存档列出相应的条目  
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/10DrawerMenu.png" width = "360" height = "640" alt="Keep"/>
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/31Color.png" width = "360" height = "640" alt="Keep"/>
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/33Archived.png" width = "360" height = "640" alt="Keep"/>
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/34Place.png" width = "360" height = "640" alt="Keep"/>
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/35Label.png" width = "360" height = "640" alt="Keep"/>

### 标签管理（增删改查）
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/11Labels.png" width = "360" height = "640" alt="Keep"/>

### 通过 Google Place 添加位置
调用的是 [Google Places API](https://developers.google.com/places/?hl=zh-cn)
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/12AutoPlace.png" width = "360" height = "640" alt="Keep"/>
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/13PickPlace.png" width = "360" height = "640" alt="Keep"/>

### 条目编辑页面菜单
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/20Menu.png" width = "360" height = "640" alt="Keep"/>

### 设置编辑时文字的透明度
旁边有奇怪的人时可使用...  
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/21TextAlpha.png" width = "360" height = "640" alt="Keep"/>

### 图片查看页面
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/22Photo.png" width = "360" height = "640" alt="Keep"/>

### 选择文件夹（颜色）
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/23PickColor.png" width = "360" height = "640" alt="Keep"/>

### 抽屉菜单
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/30DrawerMenu.png" width = "360" height = "640" alt="Keep"/>

### 滑动卡片存档条目
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/32Archive.png" width = "360" height = "640" alt="Keep"/>

### 备份和还原数据（SDCard/Dropbox）
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/40SDCard.png" width = "360" height = "640" alt="Keep"/>
<img src="https://raw.githubusercontent.com/keng42/Keep/master/screenshots/41Dropbox.png" width = "360" height = "640" alt="Keep"/>
