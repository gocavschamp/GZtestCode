# 3D 象棋「吃子过场动画」SVGA 化方案（方案 B：原生覆盖层 + JS Bridge）

> 状态：设计定稿，进入实施
> 关联文件：`h5/chinese_chess.html`、`ui/kids/KidsH5GameActivity.kt`、`res/layout/activity_kids_h5_game.xml`

---

## 1. 背景与目标

`chinese_chess.html`（Three.js 3D 象棋）目前吃子时会播放一段「角色特写过场」：在 WebView 内用**第二个 WebGL 渲染器**（`#csCanvas`）实时渲染 1.5s 的 3D 动画（棋子冲锋、粒子、冲击波、炮弹）。代码注释自述"类似 SVGA 转场"。

目标：将这段过场改为 **SVGA 预制动画**，由**原生侧覆盖层**播放，以获得：

- 原生硬件解码，播放链路与现有礼物/胜利动画一致；
- 复用工程已有 `SvgaInitUtil` + `SVGADynamicEntity`（动态立绘/文字）；
- 内存与生命周期原生可控；
- H5 仅保留极轻的 JS Bridge 调用，3D 实时过场保留为**降级路径**。

## 2. 现状分析

| 项 | 现状 |
|---|---|
| 触发点 | `doHumanMove`（L861）与 `doAiMove`（L918）均调用 `playCaptureCutscene(side,type,side,type)` |
| 播放器 | `#cutscene` 覆盖层 + `#csCanvas`（第二个 THREE.WebGLRenderer），`cs.dur=1.5s` |
| 动态内容 | 吃子方/被吃方的棋子类型（7 种）× 阵营（红/黑）运行时变化 |
| 原生基建 | `com.github.yyued:SVGAPlayer-Android:2.6.1`（经 basetools 传递依赖），`SvgaInitUtil` 支持 assets 解码 + 动态文本/图片 |
| JS Bridge | `KidsH5GameActivity` 目前**无**任何桥，需新增 |

棋子类型常量（H5）：`KING=0, ADVISOR=1, ELEPHANT=2, HORSE=3, ROOK=4, CANNON=5, PAWN=6`。

## 3. 总体架构

```
┌──────────────────────── WebView (chinese_chess.html) ────────────────────────┐
│ 吃子 → playCaptureCutscene() → window.AndroidBridge.playCapture(...) 返回1？ │
│        是 → 原生接管，跳过 3D 过场              否 → 原 3D 过场（降级）         │
└───────────────▲──────────────────────────────────────────────────┘
                │ @JavascriptInterface（WebView 子线程，同步返回）
┌───────────────┴────────────────── 原生 KidsH5GameActivity ───────────────────┐
│ SVGAImageView 覆盖层（root FrameLayout 最上层，默认 GONE，不可点击不拦触摸）    │
│ 预加载：进入游戏时解析 svga/chess_cutscene.svga → 缓存 SVGAVideoEntity          │
│ 播放：动态替换立绘(cs_*.png) + 标题文字 → startAnimation → onFinished → GONE    │
│ 清理：onDestroy 停止播放、释放 drawable、移除桥、parser.onDestroy()            │
└───────────────────────────────────────────────────────────────────────────────┘
```

关键设计决策：

1. **原生播放不阻塞游戏逻辑**：与现有 3D 过场行为对齐（过场期间 `busy` 由走子流程自行管理），覆盖层纯视觉、`clickable=false` 不拦触摸，无需 H5 挂起/恢复协议，无死锁风险。
2. **同步返回接管结果**：桥方法在 WebView 子线程同步构造 `SVGADrawable`（实体已预加载），返回 `1` 表示原生接管、`0` 表示降级。H5 依据返回值决定是否跳过 3D 过场。
3. **动态内容用「通用模板 + 动态替换」**：一套模板 SVGA + 运行时替换立绘/文字，避免 7 种棋子 × 2 阵营全量预制。

## 4. 通信协议

### 4.1 H5 → 原生（唯一入口）

```
window.AndroidBridge.playCapture(eaterSide, eaterType, eatenSide, eatenType) → Int
```

- `eaterSide/eatenSide`：`"red"` / `"black"`（String）
- `eaterType/eatenType`：`0..6`（Int，对应 H5 棋子类型常量）
- 返回 `1` = 原生已接管播放；`0` = 未接管（H5 走 3D 降级）

### 4.2 原生 → H5（本阶段不需要）

原生播放为纯视觉叠加，结束后仅隐藏覆盖层，不回调 H5。

## 5. H5 侧改造（chinese_chess.html）

`playCaptureCutscene()` 函数入口处增加桥接管分支，其余逻辑不动：

```js
function playCaptureCutscene(eaterSide,eaterType,eatenSide,eatenType){
  // 方案B：原生 SVGA 覆盖层接管；未接管则降级到 3D 过场
  if(window.AndroidBridge && window.AndroidBridge.playCapture){
    try{
      if(window.AndroidBridge.playCapture(eaterSide,eaterType,eatenSide,eatenType)===1) return;
    }catch(e){ dbg('SVGA bridge err:'+(e&&e.message)); }
  }
  // ...原 3D 过场逻辑（保留，作降级路径）
}
```

- 一处改动同时覆盖人机双方吃子（两处调用共用此函数）。
- 非 Android 环境（浏览器调试）`window.AndroidBridge` 不存在，自然走 3D 过场。

## 6. 原生侧改造（KidsH5GameActivity + activity_kids_h5_game.xml）

### 6.1 布局

`activity_kids_h5_game.xml` 在根 FrameLayout 中、`btnExit` 之后追加全屏覆盖层：

```xml
<com.opensource.svgaplayer.SVGAImageView
    android:id="@+id/svgaCutscene"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:clickable="false"
    android:focusable="false"
    android:visibility="gone" />
```

- 置于最上层但不拦截触摸（非 clickable，事件穿透到 WebView）。
- 仅象棋游戏使用，其他游戏该 View 恒为 GONE。

### 6.2 桥注入（仅象棋）

```kotlin
if (file == GAME_CHESS) {
    binding.webGame.addJavascriptInterface(ChessSvgaBridge(), "AndroidBridge")
}
```

### 6.3 桥实现（同步返回接管结果）

```kotlin
private inner class ChessSvgaBridge {
    @JavascriptInterface
    fun playCapture(eaterSide: String, eaterType: Int, eatenSide: String, eatenType: Int): Int {
        val entity = svgaEntityCache ?: return 0          // 未预加载成功 → 降级
        val dynamic = buildDynamicEntity(eaterSide, eaterType, eatenSide, eatenType) ?: return 0
        val drawable = SVGADrawable(entity, dynamic)
        runOnUiThread {
            binding.svgaCutscene.setImageDrawable(drawable)
            binding.svgaCutscene.visibility = View.VISIBLE
            binding.svgaCutscene.callback = object : SVGACallback {
                override fun onFinished() { binding.svgaCutscene.visibility = View.GONE }
                override fun onPause() {}
                override fun onRepeat() {}
                override fun onStep(frame: Int, percentage: Double) {}
            }
            binding.svgaCutscene.startAnimation()
        }
        return 1
    }
}
```

### 6.4 动态内容映射

- 棋子名：`RED_NAMES = [帅,仕,相,马,车,炮,兵]`、`BLACK_NAMES = [将,士,象,马,车,炮,卒]`（按 type 索引）。
- 模板 SVGA 约定的动态 key：
  - 位图：`eater`（吃子方立绘）、`eaten`（被吃方立绘）
  - 文本：`title`（例：`⚔️ 吃！车`）
- 立绘路径规范（沿用 `chess_assets/` 约定，竖版）：`h5/chess_assets/cs_<type>_<side>.png`，例：`cs_rook_red.png`、`cs_pawn_black.png`。文件缺失时跳过动态替换（模板占位即默认图），不影响播放。

### 6.5 预加载与缓存

- 进入象棋页面 `onPageFinished` 后，用 `SVGAParser`（applicationContext）后台解析 `svga/chess_cutscene.svga`，成功后缓存 `SVGAVideoEntity` 到 `svgaEntityCache`。
- 首次吃子一定晚于页面加载完成，缓存必然就绪；若解析失败则缓存为 null，桥返回 0 走降级，游戏不受影响。

### 6.6 生命周期与防泄漏

`onDestroy`：
- `binding.webGame.removeJavascriptInterface("AndroidBridge")`
- `binding.svgaCutscene.stopAnimation(true)`、`setImageDrawable(null)`、`callback = null`、`visibility = GONE`
- `parser?.onDestroy()`，`svgaEntityCache = null`

## 7. 资源规范（需设计/动效产出）

| 资源 | 路径 | 说明 |
|---|---|---|
| 过场模板 | `assets/svga/chess_cutscene.svga` | 含位图 key `eater`/`eaten`、文本 key `title`；2D 卡通战场风，时长约 1.5s |
| 棋子立绘 ×14 | `assets/h5/chess_assets/cs_<type>_<side>.png` | 竖版，红/黑各 7 种，Q 版古战场角色 |

> 模板与立绘就位前：桥返回 0，游戏自动走原 3D 过场（零回归）。验证桥路径可临时将任一 `.svga` 复制为 `chess_cutscene.svga` 观察覆盖层播放。

## 8. 降级与兜底

| 场景 | 行为 |
|---|---|
| 非 Android / 浏览器调试 | `window.AndroidBridge` 不存在 → 3D 过场 |
| SVGA 模板缺失/解析失败 | 桥返回 0 → 3D 过场 |
| 动态立绘缺失 | 跳过动态替换，用模板默认图 |
| 播放中再次触发吃子 | 不可能（走子受 `busy` 串行保护）；为稳妥起见覆盖层重新 setDrawable 覆盖播放 |

## 9. 性能与内存

- 实体缓存避免重复 decode（首次吃子零等待）。
- 覆盖层播放完毕即 `GONE` + 清 drawable，避免 WebView 表面叠加长期驻留。
- 桥方法在 WebView 子线程仅做对象构造 + 资源读取，主线程只做 set/start 轻量操作。
- 立绘加载失败不重试、不阻塞。

## 10. 验证清单

1. Android 真机进「3D 象棋」，红/黑双方各吃一子 → 观察覆盖层 SVGA 播放且结束后消失。
2. 原生接管后 H5 不再弹出 `#cutscene` 3D 过场；日志（KidsH5）无 bridge 异常。
3. 删掉/改名 `chess_cutscene.svga` 复测 → 自动回退 3D 过场，游戏流程无异常。
4. 连续吃子、对局结束（结算面板 z-index 高于覆盖层）均正常。
5. 退出游戏无泄漏（日志无异常、覆盖层已 GONE）。
