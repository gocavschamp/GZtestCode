---
name: "project-architecture"
description: "FirstApplication 项目架构速查：模块划分、技术栈、Activity/Fragment 使用规范、路由与新增页面步骤。在需要了解项目结构、新增页面、查找功能入口或理解现有代码组织方式时调用。"
---

# FirstApplication 项目架构速查

一个 Android 特效/自定义组件演示工程（多模块），已扩展**幼儿教育模块**（数字/汉字学习）。用于快速了解代码组织、遵循既有模式进行开发。

## 1. 模块结构（settings.gradle.kts）

| 模块 | 职责 | 包名 |
|------|------|------|
| `:app` | 主 App / 演示入口，所有 demo Activity 与 Fragment，幼儿教育模块 | `com.example.firstapplication` |
| `:baselibray` | 基础库：自定义 View（Shimmer/Rolling/Countdown/Score）、PAG/SVGA 辅助、浮窗通知、国旗、MineActivity | `com.ywm.baselibray` |
| `:baseapi` | 跨模块 API：FloatViewRouter / UploadFileRouter / UploadFileService | `com.example.baseapi` |
| `:basetools` | 工具库（依赖子采样图片库，构建时被排除） | `com.example.basetools` |
| `:homelogic` | 首页业务逻辑（Manifest 中已注释停用） | `com.example.homelogic` |
| `:easyPhotos` | 第三方图片选择器源码 | `com.huantansheng.easyphotos` |
| `:animplayer` | 腾讯 VAP 动画播放器源码 | `com.tencent.qgame.animplayer` |

依赖关系（app/build.gradle.kts）：`app` 依赖 `homelogic`、`baseapi`、`basetools`、`baselibray`、`animplayer`。

## 2. 技术栈

- **语言**：Kotlin 2.2.0（少量 Java 第三方源码）
- **构建**：AGP 8.13.0 + Gradle Kotlin DSL（`libs.versions.toml` 统一管理版本）
- **UI**：AndroidX（appcompat / core-ktx / swiperefreshlayout）、Material 1.10（Chip/TabLayout/MaterialCardView）、ConstraintLayout、Flexbox
- **ViewBinding**：全模块 `enable = true`，新建布局自动生成 `ActivityXxxBinding` / `FragmentXxxBinding` / `ItemXxxBinding`
- **路由**：ARouter 1.5.2（kapt 编译期扫描，`AROUTER_MODULE_NAME` 按模块生成路由表）
- **动画/特效**：SVGA（SVGAPlayer）、Lottie、PAG、VAP（animplayer 源码）、Skeleton 骨架屏 + ShimmerLayout
- **数据存储**：Room 2.8.x（kapt，幼儿教育模块本地数据库）
- **图片**：Glide + WebpDecoder
- **工具**：Blankj utilcodex、LeakCanary
- **minSdk 24 / targetSdk 36 / compileSdk 36 / Java 11**

## 3. 入口

- **Application**：`app/src/main/java/com/example/firstapplication/MyApp.kt`
  - `Utils.init(this)` → `ARouter.openDebug/openLog` → `ARouter.init(this)` → `FloatViewRouter.initFloatView(this)`
- **Launcher Activity**：`app/src/main/java/com/example/firstapplication/ui/kids/KidsHomeActivity.kt`（幼儿教育首页）
  - `@Route(path = "/module/kids/home")`，Manifest 中为 `MAIN/LAUNCHER`（竖屏）
  - 原 MainActivity（`/module/main`，`launchMode=singleTask`）降级为"演示中心"，由首页底部 `cardDemo` 进入；MainActivity 内按钮 `binding.kidsHome` 仍可返回幼儿教育首页
- **主 Manifest**：`app/src/main/AndroidManifest.xml`（所有 activity 需在此注册）
- **版本目录**：`gradle/libs.versions.toml`

## 3.5 幼儿教育模块（Kids）

> 第二版：App 主入口改为幼儿教育首页，本地存储升级为 Room 数据库，新增本地语音朗读（TTS），内容扩充。

### 代码位置

```
app/src/main/java/com/example/firstapplication/ui/kids/
├── KidsHomeActivity.kt          # 幼儿教育首页（App 启动页 LAUNCHER，五张彩色卡片：数字/加减法/汉字/H5小游戏/古诗学堂 + 演示中心入口，进度上限 数字50/汉字500；动画：太阳旋转/云朵漂移/星星飘落/气球上升/卡片滑入入场/点击回弹/进度条平滑滚动，onDestroy 时 isActive 停循环）
├── NumberLearningActivity.kt    # 数字乐园（认识数字 0-999 四级：3-5岁0-200/5-7岁0-500/7-9岁0-999/🎲随机0-999，含中文读法/朗读；认识数字进度本地记录：级别切换从上一级别结束数字继续、下次进入恢复、↺从0重置按钮；找数字"听音选数"；数一数"多彩图标点数"16种图标）
├── NumberArithmeticActivity.kt  # 加减法（难度分级 ≤10 / ≤20 / ≤100，统计卡片：得分/连胜/最佳 + 10题一局进度条，出题朗读🔊按钮、10 题一局统计、连胜解锁）
├── ChineseWordActivity.kt       # 汉字乐园（TabLayout 五页：基础笔画/笔画演示/汉字识字500/仿写描红/每日一句，点击朗读并标记学会）
├── KidsH5Activity.kt            # H5 小游戏容器（WebView 加载本地 assets/h5，三个游戏切换按钮）
├── PoetryActivity.kt            # 古诗学堂（列表页 50 首中小学课本古诗 → 详情页：整首朗读 / 每字点击朗读字+拼音，每首诗意渐变背景 + 内容相关 emoji 装饰 + 释义解说）
├── KidsStatusBar.kt             # 沉浸式状态栏工具（透明状态栏 + 内容延伸到状态栏 + 根布局自动避让）
├── HanziLibrary.kt              # 汉字数据源：34 精细教学字 + 500 常用字（EXTENDED_HANZI）+ 8 基础笔画 + 44 条每日句子
├── PoetryLibrary.kt             # 古诗数据源：50 首中小学课本古诗（标题/作者/逐字拼音/释义/主题渐变配色/背景 emoji）
├── StrokeAnimationView.kt       # 自定义 View：田字格笔画顺序动画（内置 34 个汉字 + 8 个基础笔画数据）
├── TracingView.kt               # 自定义 View：仿写描红，触摸笔迹，PNG 本地保存
├── KidsProgressStore.kt         # Room 数据门面（同步 API + runBlocking 包装），含第一版 SharedPreferences 迁移
├── KidsTts.kt                   # 本地语音朗读（系统 TextToSpeech 中文引擎）
└── db/KidsDatabase.kt           # Room：Database + 3 个 DAO（进度键值 / 已学汉字 / 答题记录）
    db/KidsEntities.kt           # Entity：StudyProgressEntity / LearnedCharEntity / ArithmeticRecordEntity
```

### 布局 & 资源

- `res/layout/activity_kids_home.xml`（启动页，含四张进度卡片 + 演示中心入口）、`activity_number_learning.xml`（三模式切换：认识/找数字/数一数 + 四级难度chip）、`activity_number_arithmetic.xml`（统计卡片 + 进度条 + 朗读喇叭）、`activity_chinese_word.xml`（五页：基础笔画/笔画演示/识字网格/仿写/每日一句）、`activity_kids_h5.xml`（H5 游戏容器：三按钮 + WebView）、`activity_poetry.xml`（古诗学堂：列表页 RecyclerView + 详情页 FrameLayout 双页切换，bgLayer 渐变背景层 + decorLayer emoji 装饰层 + 逐字诗句容器）、`item_poetry.xml`（古诗列表 item：诗意渐变圆底 emoji + 标题/作者/首句预览）、`item_kids_char_grid.xml`（识字网格 item，含 ✅ 学会标记）
- `assets/h5/number_match.html`（数字翻牌记忆配对，纯 HTML/JS 本地小游戏）、`assets/h5/hanzi_link.html`（汉字连连看，经典 0/1/2 拐点连通判定，可绕外圈，提示时高亮闪烁标记待连字）、`assets/h5/puzzle.html`（3x3 emoji 滑动拼图，随机滑动保证有解，步数统计）
- **沉浸式**：幼儿教育 6 个 Activity 使用 `Theme.Kids`（透明状态栏）+ `KidsStatusBar.immersive()`（内容延伸到状态栏，根布局顶部自动避开状态栏高度）
- `res/drawable/bg_kids_*.xml`（渐变背景）、`bg_card_poetry.xml`（古诗卡片橙金渐变）、`bg_home_icon.xml`（圆形图标底）、`bg_speak_btn.xml`（喇叭按钮）、`bg_char_cell.xml`（汉字卡片格）
- `res/values/colors.xml` 中 `kids_*` 前缀的儿童清新色板
- 入口：**App 启动即 KidsHomeActivity**（Manifest LAUNCHER），内部卡片跳五个子页（数字/加减法/汉字/H5/古诗）；`cardDemo` → ARouter `/module/main` 进演示中心（原 MainActivity）
- Manifest 已注册全部 6 个 Activity（竖屏 portrait），MainActivity 不再是 launcher

### 设计约定（第二版）

- **主入口**：KidsHomeActivity = LAUNCHER；MainActivity（`/module/main`）仅作为演示中心，通过首页底部卡片进入
- **数据存储**：Room 数据库 `kids_learning.db`（Room 2.8.x，kapt），见 `db/`。`KidsProgressStore` 对外保持同步 API
- **语音**：`KidsTts` 系统 TextToSpeech 中文引擎（本地播放）。数字页切换/出题、加减法出题/答对/🔊喇叭、汉字卡片点击、每日一句均触发朗读
- **风格**：渐变背景 + emoji 装饰 + 圆角 MaterialCardView + LinearProgressIndicator 进度条，无网络图片离线运行
- **动画**：纯原生 ValueAnimator/属性动画（数字弹跳、笔画 Path 绘制、描红实时笔迹、分数缩放）
- **汉字笔画数据**：`StrokeAnimationView.StrokeData.charStrokes`，坐标为 0-100 归一化，新增汉字只需追加笔画点数组；基础笔画页复用同名"横/竖/撇/捺/点/提/横折/竖钩"数据
- **仿写保存**：`TracingView.saveToLocal()` 输出 PNG 到 `filesDir/kids_tracing/`，保存时自动 markCharLearned
- **趣味性**：数字乐园 3 种玩法（认识 0-999 四级难度 + 🎲随机 + 找数字 + 数一数 16 种图标）+ 加减法 10 题一局统计、连胜解锁更高难度、答对随机鼓励语 + TTS 播报；每日一句按时间取模定位、可切换并朗读；H5 本地小游戏（数字翻牌 / 汉字连连看 / emoji 拼图，WebView 加载 assets）

### 后续扩展点

- 数字认识/加减法难度自动切换逻辑在 Activity 内
- 汉字笔画动画依赖 `StrokeData` 预设数据，想支持任意汉字需引入字库/字体渲染方案
- Room 需要升级表结构时：修改 `KidsDatabase.version` 并提供 `Migration`，或卸载重装（测试期）
- 如需联网素材：Glide 已可用，INTERNET 权限已声明

## 4. Activity 使用规范（两种风格，优先风格 A）

### 风格 A：ViewBinding（项目主流，:app 模块）

```kotlin
@Route(path = "/module/xxx")
class XxxActivity : AppCompatActivity() {
    private lateinit var binding: ActivityXxxBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityXxxBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
```

参考实现：CountdownActivity / SvgaActivity / PagActivity / ScaleDemoActivity / MainActivity。

### 风格 B：直接 setContentView（:baselibray 的 MineActivity）

```kotlin
@Route(path = "/other/mine")
class MineActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mine)
    }
}
```

### Manifest 注册（必须，漏了会崩）

```xml
<activity
    android:name=".ui.XxxActivity"
    android:configChanges="keyboardHidden|orientation"
    android:windowSoftInputMode="adjustResize"
    android:screenOrientation="portrait" />
```

### 页面跳转（两种方式）

```kotlin
// 方式 1：ARouter（推荐，跨模块）
ARouter.getInstance().build("/module/xxx").navigation()

// 方式 2：原生 Intent
startActivity(Intent(this, XxxActivity::class.java))
```

### ARouter 路由约定

- 路由常量：`/module/xxx`（页面）、`/service/xxx`（服务）
- 服务类实现：`@Route(path = "/service/floatview")`，跨模块用接口 + Router 门面类调用
  - 例：`baseapi` 定义 `FloatViewService` 接口，`baselibray` 的 `FloatViewServiceImpl` 实现路由，`FloatViewRouter` 作为静态门面
- 新增模块时记得在 `build.gradle.kts` 加 `kapt { arguments { arg("AROUTER_MODULE_NAME", project.name) } }`

## 5. Fragment 使用规范

当前仅 `ScaleDemoActivity` 使用 ViewPager2 + Fragment（`ui/fragment/` 包下）：

- **Adapter**：`ViewPagerAdapter : FragmentStateAdapter(fragmentActivity)`，内置 `fragments` 列表与 `getTabTitle(position)`
- **Fragment 模板**（ViewBinding + `_binding` 置空防泄漏）：

```kotlin
class Fragment1 : Fragment() {
    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater, container, savedInstanceState): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 初始化 RecyclerView 等
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

- **TabLayout 关联**：

```kotlin
TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
    tab.text = viewPagerAdapter.getTabTitle(position)
}.attach()
```

- Fragment 内解决与 ViewPager2 的滑动冲突：使用 `onItemTouchListener` + `canScrollHorizontally` 判断边界，控制 `viewPager.isUserInputEnabled`。

## 6. 新增页面完整步骤（4 步）

1. **布局**：在 `app/src/main/res/layout/` 新建 `activity_xxx.xml`
2. **Activity**：在 `app/src/main/java/com/example/firstapplication/ui/` 新建 `XxxActivity.kt`（风格 A + `@Route`)
3. **Manifest**：在 `app/src/main/AndroidManifest.xml` 注册该 activity
4. **入口**：从任一现有页面用 `ARouter.getInstance().build("/module/xxx").navigation()` 跳入；如需在 MainActivity 首页加按钮，在 `initViews()` 中仿照 `binding.countdown.setOnClickListener` 添加

## 7. 常用构建命令

```bash
./gradlew :app:installDebug   # 安装 debug 包
./gradlew :app:assembleDebug  # 构建 debug APK
./gradlew :app:assembleRelease
```

产物：`app/build/outputs/apk/debug/app-debug.apk`。

## 8. 自定义 View 约定

- 全部放在 `baselibray/src/main/java/com/ywm/baselibray/weiget/`（注意拼写：weiget 而非 widget）
- 继承 `AppCompatTextView` / `AppCompatImageView` 等，用 `@JvmOverloads constructor` + `obtainStyledAttributes(attrs, R.styleable.Xxx, 0, 0)`
- 自定义属性在 `baselibray/src/main/res/values/attrs.xml` 的 `<declare-styleable name="Xxx">` 注册，使用时 `app:attrName`
- 参考：ShimmerColorTextView / RollingTextView / CountdownTextView / ScoreBoardView

## 9. 注意事项

- `:app` 依赖 `basetools` 时排除了 `subsampling-scale-image-view`（存在冲突），新增依赖注意避开
- `MyApp` 中 ARouter 调试模式被无条件开启（openDebug），Release 前应改为仅 DEBUG 开启
- `homelogic` 模块当前未启用（Manifest 中 `HomeActivity` 被注释）
- 系统未安装 npm/Node，本项目纯 Gradle 构建，无需 node
