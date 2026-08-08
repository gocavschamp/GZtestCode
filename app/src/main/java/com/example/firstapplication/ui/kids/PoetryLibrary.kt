package com.example.firstapplication.ui.kids

/**
 * 古诗学堂数据源：中小学语文课本经典古诗（50 首）
 * 每首诗含标题、作者、逐字拼音、释义，以及贴合诗意的主题背景（渐变配色 + 背景元素）
 */
object PoetryLibrary {

    data class PoemChar(val char: String, val pinyin: String)

    data class PoemLine(val chars: List<PoemChar>) {
        /** 整行文本（含标点），用于整首朗读 */
        val text: String get() = chars.joinToString("") { it.char }
    }

    data class Poem(
        val title: String,
        val author: String,
        val lines: List<PoemLine>,
        val bgEmoji: String,
        val startColor: Int,
        val endColor: Int,
        val meaning: String
    )

    // 便捷构造：一行诗句 + 对应拼音（用空格隔开，标点用 _ 标记不显示拼音）
    private fun line(textAndPinyin: String): PoemLine {
        val parts = textAndPinyin.trim().split(" ")
        val chars = parts.map { part ->
            val index = part.indexOf('|')
            if (index >= 0) {
                PoemChar(part.substring(0, index), part.substring(index + 1))
            } else {
                PoemChar(part, "")
            }
        }
        return PoemLine(chars)
    }

    val POEMS = listOf(
        // ==================== 现有 12 首 ====================
        Poem(
            title = "静夜思", author = "李白",
            lines = listOf(
                line("床|chuáng 前|qián 明|míng 月|yuè 光|guāng ，|_"),
                line("疑|yí 是|shì 地|dì 上|shàng 霜|shuāng 。|_"),
                line("举|jǔ 头|tóu 望|wàng 明|míng 月|yuè ，|_"),
                line("低|dī 头|tóu 思|sī 故|gù 乡|xiāng 。|_")
            ),
            bgEmoji = "🌙", startColor = 0xFF1B3B5C.toInt(), endColor = 0xFF0A1B2E.toInt(),
            meaning = "夜深人静，望着明亮的月光，思念远方的故乡。"
        ),
        Poem(
            title = "春晓", author = "孟浩然",
            lines = listOf(
                line("春|chūn 眠|mián 不|bù 觉|jué 晓|xiǎo ，|_"),
                line("处|chù 处|chù 闻|wén 啼|tí 鸟|niǎo 。|_"),
                line("夜|yè 来|lái 风|fēng 雨|yǔ 声|shēng ，|_"),
                line("花|huā 落|luò 知|zhī 多|duō 少|shǎo 。|_")
            ),
            bgEmoji = "🌸", startColor = 0xFF7FB69B.toInt(), endColor = 0xFF5A7F8A.toInt(),
            meaning = "春天的早晨睡醒了，处处听到鸟鸣，一夜风雨过后，花落了多少呢？"
        ),
        Poem(
            title = "咏鹅", author = "骆宾王",
            lines = listOf(
                line("鹅|é 鹅|é 鹅|é ，|_"),
                line("曲|qū 项|xiàng 向|xiàng 天|tiān 歌|gē 。|_"),
                line("白|bái 毛|máo 浮|fú 绿|lǜ 水|shuǐ ，|_"),
                line("红|hóng 掌|zhǎng 拨|bō 清|qīng 波|bō 。|_")
            ),
            bgEmoji = "🦢", startColor = 0xFF3FA9C4.toInt(), endColor = 0xFF1B5E7A.toInt(),
            meaning = "大白鹅弯着脖子向天歌唱，白毛浮在绿水，红掌拨动清波。"
        ),
        Poem(
            title = "悯农（其二）", author = "李绅",
            lines = listOf(
                line("锄|chú 禾|hé 日|rì 当|dāng 午|wǔ ，|_"),
                line("汗|hàn 滴|dī 禾|hé 下|xià 土|tǔ 。|_"),
                line("谁|shéi 知|zhī 盘|pán 中|zhōng 餐|cān ，|_"),
                line("粒|lì 粒|lì 皆|jiē 辛|xīn 苦|kǔ 。|_")
            ),
            bgEmoji = "🌾", startColor = 0xFFE0A83C.toInt(), endColor = 0xFF9A6B1F.toInt(),
            meaning = "中午烈日下锄地，汗水滴进禾下的土里，盘中的粮食粒粒都来之不易。"
        ),
        Poem(
            title = "登鹳雀楼", author = "王之涣",
            lines = listOf(
                line("白|bái 日|rì 依|yī 山|shān 尽|jìn ，|_"),
                line("黄|huáng 河|hé 入|rù 海|hǎi 流|liú 。|_"),
                line("欲|yù 穷|qióng 千|qiān 里|lǐ 目|mù ，|_"),
                line("更|gèng 上|shàng 一|yì 层|céng 楼|lóu 。|_")
            ),
            bgEmoji = "🏯", startColor = 0xFFF98A4B.toInt(), endColor = 0xFFA33A28.toInt(),
            meaning = "夕阳傍着群山落下，黄河奔向大海。想要看得更远，就要站得更高。"
        ),
        Poem(
            title = "江南", author = "汉乐府",
            lines = listOf(
                line("江|jiāng 南|nán 可|kě 采|cǎi 莲|lián ，|_"),
                line("莲|lián 叶|yè 何|hé 田|tián 田|tián 。|_"),
                line("鱼|yú 戏|xì 莲|lián 叶|yè 间|jiān 。|_"),
                line("鱼|yú 戏|xì 莲|lián 叶|yè 东|dōng ，|_"),
                line("鱼|yú 戏|xì 莲|lián 叶|yè 西|xī ，|_"),
                line("鱼|yú 戏|xì 莲|lián 叶|yè 南|nán ，|_"),
                line("鱼|yú 戏|xì 莲|lián 叶|yè 北|běi 。|_")
            ),
            bgEmoji = "🪷", startColor = 0xFF58B368.toInt(), endColor = 0xFF1B6E5E.toInt(),
            meaning = "江南水乡可采莲，莲叶茂密，鱼儿在莲叶间游来游去。"
        ),
        Poem(
            title = "画", author = "王维",
            lines = listOf(
                line("远|yuǎn 看|kàn 山|shān 有|yǒu 色|sè ，|_"),
                line("近|jìn 听|tīng 水|shuǐ 无|wú 声|shēng 。|_"),
                line("春|chūn 去|qù 花|huā 还|hái 在|zài ，|_"),
                line("人|rén 来|lái 鸟|niǎo 不|bù 惊|jīng 。|_")
            ),
            bgEmoji = "🖼️", startColor = 0xFF4E8579.toInt(), endColor = 0xFF27493F.toInt(),
            meaning = "远看山有颜色，近听水没有声音——原来这是一幅美丽的画。"
        ),
        Poem(
            title = "草", author = "白居易",
            lines = listOf(
                line("离|lí 离|lí 原|yuán 上|shàng 草|cǎo ，|_"),
                line("一|yí 岁|suì 一|yì 枯|kū 荣|róng 。|_"),
                line("野|yě 火|huǒ 烧|shāo 不|bú 尽|jìn ，|_"),
                line("春|chūn 风|fēng 吹|chuī 又|yòu 生|shēng 。|_")
            ),
            bgEmoji = "🌱", startColor = 0xFF6CC342.toInt(), endColor = 0xFF1F7A2E.toInt(),
            meaning = "原野上的草一岁一枯荣，野火烧不尽，春风一吹又长出来。"
        ),
        Poem(
            title = "望庐山瀑布", author = "李白",
            lines = listOf(
                line("日|rì 照|zhào 香|xiāng 炉|lú 生|shēng 紫|zǐ 烟|yān ，|_"),
                line("遥|yáo 看|kàn 瀑|pù 布|bù 挂|guà 前|qián 川|chuān 。|_"),
                line("飞|fēi 流|liú 直|zhí 下|xià 三|sān 千|qiān 尺|chǐ ，|_"),
                line("疑|yí 是|shì 银|yín 河|hé 落|luò 九|jiǔ 天|tiān 。|_")
            ),
            bgEmoji = "🏔️", startColor = 0xFF6E93A8.toInt(), endColor = 0xFF2C4A66.toInt(),
            meaning = "香炉峰升起紫色烟雾，瀑布像银河一样从九天飞落下来。"
        ),
        Poem(
            title = "古朗月行（节选）", author = "李白",
            lines = listOf(
                line("小|xiǎo 时|shí 不|bù 识|shí 月|yuè ，|_"),
                line("呼|hū 作|zuò 白|bái 玉|yù 盘|pán 。|_"),
                line("又|yòu 疑|yí 瑶|yáo 台|tái 镜|jìng ，|_"),
                line("飞|fēi 在|zài 青|qīng 云|yún 端|duān 。|_")
            ),
            bgEmoji = "🌕", startColor = 0xFF5A3D7A.toInt(), endColor = 0xFF1F1738.toInt(),
            meaning = "小时候不认识月亮，把它叫做白玉盘，又像是飞在青云端的瑶台仙镜。"
        ),
        Poem(
            title = "绝句", author = "杜甫",
            lines = listOf(
                line("两|liǎng 个|gè 黄|huáng 鹂|lí 鸣|míng 翠|cuì 柳|liǔ ，|_"),
                line("一|yì 行|háng 白|bái 鹭|lù 上|shàng 青|qīng 天|tiān 。|_"),
                line("窗|chuāng 含|hán 西|xī 岭|lǐng 千|qiān 秋|qiū 雪|xuě ，|_"),
                line("门|mén 泊|bó 东|dōng 吴|wú 万|wàn 里|lǐ 船|chuán 。|_")
            ),
            bgEmoji = "🐦", startColor = 0xFF5FB3A8.toInt(), endColor = 0xFF2F6FA8.toInt(),
            meaning = "黄鹂在翠柳间鸣叫，白鹭飞上青天，窗外积雪，门外停着远航的船。"
        ),
        Poem(
            title = "游子吟", author = "孟郊",
            lines = listOf(
                line("慈|cí 母|mǔ 手|shǒu 中|zhōng 线|xiàn ，|_"),
                line("游|yóu 子|zǐ 身|shēn 上|shàng 衣|yī 。|_"),
                line("临|lín 行|xíng 密|mì 密|mì 缝|féng ，|_"),
                line("意|yì 恐|kǒng 迟|chí 迟|chí 归|guī 。|_"),
                line("谁|shéi 言|yán 寸|cùn 草|cǎo 心|xīn ，|_"),
                line("报|bào 得|dé 三|sān 春|chūn 晖|huī 。|_")
            ),
            bgEmoji = "🪡", startColor = 0xFFE8A05C.toInt(), endColor = 0xFF8F4E2A.toInt(),
            meaning = "母亲为远行的孩子细细缝衣，寸草般的孝心，难报春天般的母爱。"
        ),

        // ==================== 新增小学古诗 ====================
        Poem(
            title = "咏柳", author = "贺知章",
            lines = listOf(
                line("碧|bì 玉|yù 妆|zhuāng 成|chéng 一|yī 树|shù 高|gāo ，|_"),
                line("万|wàn 条|tiáo 垂|chuí 下|xià 绿|lǜ 丝|sī 绦|tāo 。|_"),
                line("不|bù 知|zhī 细|xì 叶|yè 谁|shéi 裁|cái 出|chū ，|_"),
                line("二|èr 月|yuè 春|chūn 风|fēng 似|sì 剪|jiǎn 刀|dāo 。|_")
            ),
            bgEmoji = "🌿", startColor = 0xFF66BB6A.toInt(), endColor = 0xFF2E7D32.toInt(),
            meaning = "高高的柳树像碧玉妆成，万条柳枝像绿丝带垂下来。那细细的柳叶是谁裁出的？是二月的春风这把剪刀。"
        ),
        Poem(
            title = "村居", author = "高鼎",
            lines = listOf(
                line("草|cǎo 长|zhǎng 莺|yīng 飞|fēi 二|èr 月|yuè 天|tiān ，|_"),
                line("拂|fú 堤|dī 杨|yáng 柳|liǔ 醉|zuì 春|chūn 烟|yān 。|_"),
                line("儿|ér 童|tóng 散|sàn 学|xué 归|guī 来|lái 早|zǎo ，|_"),
                line("忙|máng 趁|chèn 东|dōng 风|fēng 放|fàng 纸|zhǐ 鸢|yuān 。|_")
            ),
            bgEmoji = "🪁", startColor = 0xFF64B5F6.toInt(), endColor = 0xFF2E7D32.toInt(),
            meaning = "春天青草生长黄莺飞舞，杨柳拂堤如醉在春烟里。孩子们放学早早回家，趁着东风放起风筝。"
        ),
        Poem(
            title = "悯农（其一）", author = "李绅",
            lines = listOf(
                line("春|chūn 种|zhòng 一|yī 粒|lì 粟|sù ，|_"),
                line("秋|qiū 收|shōu 万|wàn 颗|kē 子|zǐ 。|_"),
                line("四|sì 海|hǎi 无|wú 闲|xián 田|tián ，|_"),
                line("农|nóng 夫|fū 犹|yóu 饿|è 死|sǐ 。|_")
            ),
            bgEmoji = "🌾", startColor = 0xFFE8B84B.toInt(), endColor = 0xFF8D6E1F.toInt(),
            meaning = "春天种下一粒种子，秋天收获万颗粮食。四海之内没有荒田，可辛苦的农夫还是会饿死。"
        ),
        Poem(
            title = "小池", author = "杨万里",
            lines = listOf(
                line("泉|quán 眼|yǎn 无|wú 声|shēng 惜|xī 细|xì 流|liú ，|_"),
                line("树|shù 阴|yīn 照|zhào 水|shuǐ 爱|ài 晴|qíng 柔|róu 。|_"),
                line("小|xiǎo 荷|hé 才|cái 露|lù 尖|jiān 尖|jiān 角|jiǎo ，|_"),
                line("早|zǎo 有|yǒu 蜻|qīng 蜓|tíng 立|lì 上|shàng 头|tóu 。|_")
            ),
            bgEmoji = "🌱", startColor = 0xFF4DB6AC.toInt(), endColor = 0xFF1B5E20.toInt(),
            meaning = "泉眼悄悄流出细细的水流，树阴倒映水面显得晴柔可爱。小荷才露出尖尖的角，早有蜻蜓立在上面。"
        ),
        Poem(
            title = "赠汪伦", author = "李白",
            lines = listOf(
                line("李|lǐ 白|bái 乘|chéng 舟|zhōu 将|jiāng 欲|yù 行|xíng ，|_"),
                line("忽|hū 闻|wén 岸|àn 上|shàng 踏|tà 歌|gē 声|shēng 。|_"),
                line("桃|táo 花|huā 潭|tán 水|shuǐ 深|shēn 千|qiān 尺|chǐ ，|_"),
                line("不|bù 及|jí 汪|wāng 伦|lún 送|sòng 我|wǒ 情|qíng 。|_")
            ),
            bgEmoji = "⛵", startColor = 0xFF4FC3F7.toInt(), endColor = 0xFF1565C0.toInt(),
            meaning = "李白乘船将要远行，忽听岸上传来踏歌声。桃花潭水深过千尺，也比不上汪伦送我的情意深。"
        ),
        Poem(
            title = "山行", author = "杜牧",
            lines = listOf(
                line("远|yuǎn 上|shàng 寒|hán 山|shān 石|shí 径|jìng 斜|xiá ，|_"),
                line("白|bái 云|yún 生|shēng 处|chù 有|yǒu 人|rén 家|jiā 。|_"),
                line("停|tíng 车|chē 坐|zuò 爱|ài 枫|fēng 林|lín 晚|wǎn ，|_"),
                line("霜|shuāng 叶|yè 红|hóng 于|yú 二|èr 月|yuè 花|huā 。|_")
            ),
            bgEmoji = "🍁", startColor = 0xFFFF8A65.toInt(), endColor = 0xFFBF360C.toInt(),
            meaning = "沿着弯弯的石径登上寒山，白云深处还有人家。停下车是因为喜爱傍晚的枫林，经霜的枫叶比二月的花还红。"
        ),
        Poem(
            title = "清明", author = "杜牧",
            lines = listOf(
                line("清|qīng 明|míng 时|shí 节|jié 雨|yǔ 纷|fēn 纷|fēn ，|_"),
                line("路|lù 上|shàng 行|xíng 人|rén 欲|yù 断|duàn 魂|hún 。|_"),
                line("借|jiè 问|wèn 酒|jiǔ 家|jiā 何|hé 处|chù 有|yǒu ，|_"),
                line("牧|mù 童|tóng 遥|yáo 指|zhǐ 杏|xìng 花|huā 村|cūn 。|_")
            ),
            bgEmoji = "🌧️", startColor = 0xFF90A4AE.toInt(), endColor = 0xFF455A64.toInt(),
            meaning = "清明时节细雨纷纷，路上的行人愁绪满怀。问哪里有酒家？牧童远远指向杏花村。"
        ),
        Poem(
            title = "九月九日忆山东兄弟", author = "王维",
            lines = listOf(
                line("独|dú 在|zài 异|yì 乡|xiāng 为|wéi 异|yì 客|kè ，|_"),
                line("每|měi 逢|féng 佳|jiā 节|jié 倍|bèi 思|sī 亲|qīn 。|_"),
                line("遥|yáo 知|zhī 兄|xiōng 弟|dì 登|dēng 高|gāo 处|chù ，|_"),
                line("遍|biàn 插|chā 茱|zhū 萸|yú 少|shǎo 一|yī 人|rén 。|_")
            ),
            bgEmoji = "🏮", startColor = 0xFFEF6C00.toInt(), endColor = 0xFFB71C1C.toInt(),
            meaning = "独自漂泊在他乡，每到佳节更加思念亲人。遥想兄弟们今天登高，遍插茱萸却独独少了我一个人。"
        ),
        Poem(
            title = "饮湖上初晴后雨", author = "苏轼",
            lines = listOf(
                line("水|shuǐ 光|guāng 潋|liàn 滟|yàn 晴|qíng 方|fāng 好|hǎo ，|_"),
                line("山|shān 色|sè 空|kōng 蒙|méng 雨|yǔ 亦|yì 奇|qí 。|_"),
                line("欲|yù 把|bǎ 西|xī 湖|hú 比|bǐ 西|xī 子|zǐ ，|_"),
                line("淡|dàn 妆|zhuāng 浓|nóng 抹|mǒ 总|zǒng 相|xiāng 宜|yí 。|_")
            ),
            bgEmoji = "🌊", startColor = 0xFF4DD0E1.toInt(), endColor = 0xFF1A6FB5.toInt(),
            meaning = "晴天西湖水光潋滟，雨天山色空蒙也很奇妙。把西湖比作美人西施，淡妆浓抹都是那么相宜。"
        ),
        Poem(
            title = "望天门山", author = "李白",
            lines = listOf(
                line("天|tiān 门|mén 中|zhōng 断|duàn 楚|chǔ 江|jiāng 开|kāi ，|_"),
                line("碧|bì 水|shuǐ 东|dōng 流|liú 至|zhì 此|cǐ 回|huí 。|_"),
                line("两|liǎng 岸|àn 青|qīng 山|shān 相|xiāng 对|duì 出|chū ，|_"),
                line("孤|gū 帆|fān 一|yī 片|piàn 日|rì 边|biān 来|lái 。|_")
            ),
            bgEmoji = "⛰️", startColor = 0xFF5C8A8F.toInt(), endColor = 0xFF1B3A4B.toInt(),
            meaning = "天门山被长江劈开，碧水东流在这里回旋。两岸青山相对耸立，一叶孤舟从日边驶来。"
        ),
        Poem(
            title = "题西林壁", author = "苏轼",
            lines = listOf(
                line("横|héng 看|kàn 成|chéng 岭|lǐng 侧|cè 成|chéng 峰|fēng ，|_"),
                line("远|yuǎn 近|jìn 高|gāo 低|dī 各|gè 不|bù 同|tóng 。|_"),
                line("不|bù 识|shí 庐|lú 山|shān 真|zhēn 面|miàn 目|mù ，|_"),
                line("只|zhǐ 缘|yuán 身|shēn 在|zài 此|cǐ 山|shān 中|zhōng 。|_")
            ),
            bgEmoji = "🌄", startColor = 0xFF8D9BA6.toInt(), endColor = 0xFF37474F.toInt(),
            meaning = "横看是山岭侧看是山峰，远近高低各不同。看不清庐山的真面目，是因为自己就身在这座山中。"
        ),
        Poem(
            title = "元日", author = "王安石",
            lines = listOf(
                line("爆|bào 竹|zhú 声|shēng 中|zhōng 一|yī 岁|suì 除|chú ，|_"),
                line("春|chūn 风|fēng 送|sòng 暖|nuǎn 入|rù 屠|tú 苏|sū 。|_"),
                line("千|qiān 门|mén 万|wàn 户|hù 曈|tóng 曈|tóng 日|rì ，|_"),
                line("总|zǒng 把|bǎ 新|xīn 桃|táo 换|huàn 旧|jiù 符|fú 。|_")
            ),
            bgEmoji = "🧨", startColor = 0xFFE53935.toInt(), endColor = 0xFF8E0000.toInt(),
            meaning = "爆竹声中旧年过去，春风送暖喝下屠苏酒。初升的太阳照亮千家万户，人们总把旧桃符换成新桃符。"
        ),
        Poem(
            title = "凉州词", author = "王之涣",
            lines = listOf(
                line("黄|huáng 河|hé 远|yuǎn 上|shàng 白|bái 云|yún 间|jiān ，|_"),
                line("一|yī 片|piàn 孤|gū 城|chéng 万|wàn 仞|rèn 山|shān 。|_"),
                line("羌|qiāng 笛|dí 何|hé 须|xū 怨|yuàn 杨|yáng 柳|liǔ ，|_"),
                line("春|chūn 风|fēng 不|bù 度|dù 玉|yù 门|mén 关|guān 。|_")
            ),
            bgEmoji = "🏜️", startColor = 0xFFF5B041.toInt(), endColor = 0xFFA04000.toInt(),
            meaning = "黄河远远伸向白云之间，一座孤城耸立在万仞高山边。羌笛何必吹奏幽怨的折杨柳，春风也吹不到玉门关外。"
        ),
        Poem(
            title = "出塞", author = "王昌龄",
            lines = listOf(
                line("秦|qín 时|shí 明|míng 月|yuè 汉|hàn 时|shí 关|guān ，|_"),
                line("万|wàn 里|lǐ 长|cháng 征|zhēng 人|rén 未|wèi 还|huán 。|_"),
                line("但|dàn 使|shǐ 龙|lóng 城|chéng 飞|fēi 将|jiàng 在|zài ，|_"),
                line("不|bù 教|jiāo 胡|hú 马|mǎ 度|dù 阴|yīn 山|shān 。|_")
            ),
            bgEmoji = "🛡️", startColor = 0xFF78909C.toInt(), endColor = 0xFF263238.toInt(),
            meaning = "依旧是秦汉时的明月和边关，远征万里的将士还没有回来。只要飞将军李广还在，就不会让胡人骑兵越过阴山。"
        ),
        Poem(
            title = "芙蓉楼送辛渐", author = "王昌龄",
            lines = listOf(
                line("寒|hán 雨|yǔ 连|lián 江|jiāng 夜|yè 入|rù 吴|wú ，|_"),
                line("平|píng 明|míng 送|sòng 客|kè 楚|chǔ 山|shān 孤|gū 。|_"),
                line("洛|luò 阳|yáng 亲|qīn 友|yǒu 如|rú 相|xiāng 问|wèn ，|_"),
                line("一|yī 片|piàn 冰|bīng 心|xīn 在|zài 玉|yù 壶|hú 。|_")
            ),
            bgEmoji = "❄️", startColor = 0xFF90CAF9.toInt(), endColor = 0xFF1A3A5C.toInt(),
            meaning = "寒雨连江一夜洒满吴地，清晨送别友人，楚山孤独。洛阳亲友若问起我，就说我的心像玉壶里的冰一样纯洁。"
        ),
        Poem(
            title = "鹿柴", author = "王维",
            lines = listOf(
                line("空|kōng 山|shān 不|bù 见|jiàn 人|rén ，|_"),
                line("但|dàn 闻|wén 人|rén 语|yǔ 响|xiǎng 。|_"),
                line("返|fǎn 景|jǐng 入|rù 深|lín 林|lín ，|_"),
                line("复|fù 照|zhào 青|qīng 苔|tái 上|shàng 。|_")
            ),
            bgEmoji = "🌲", startColor = 0xFF43A047.toInt(), endColor = 0xFF1B5E20.toInt(),
            meaning = "空山里看不见人，只听到人说话的回响。夕阳的余晖照进深林，又映在青苔上。"
        ),
        Poem(
            title = "江雪", author = "柳宗元",
            lines = listOf(
                line("千|qiān 山|shān 鸟|niǎo 飞|fēi 绝|jué ，|_"),
                line("万|wàn 径|jìng 人|rén 踪|zōng 灭|miè 。|_"),
                line("孤|gū 舟|zhōu 蓑|suō 笠|lì 翁|wēng ，|_"),
                line("独|dú 钓|diào 寒|hán 江|jiāng 雪|xuě 。|_")
            ),
            bgEmoji = "🎣", startColor = 0xFFB3E5FC.toInt(), endColor = 0xFF37474F.toInt(),
            meaning = "群山飞鸟绝迹，路上不见人影。一叶孤舟上，披蓑戴笠的老翁，独自在寒江上垂钓。"
        ),
        Poem(
            title = "寻隐者不遇", author = "贾岛",
            lines = listOf(
                line("松|sōng 下|xià 问|wèn 童|tóng 子|zǐ ，|_"),
                line("言|yán 师|shī 采|cǎi 药|yào 去|qù 。|_"),
                line("只|zhǐ 在|zài 此|cǐ 山|shān 中|zhōng ，|_"),
                line("云|yún 深|shēn 不|bù 知|zhī 处|chù 。|_")
            ),
            bgEmoji = "🌳", startColor = 0xFF66BB6A.toInt(), endColor = 0xFF1B5E20.toInt(),
            meaning = "松树下询问童子，他说师父采药去了。就在这座山里，只是云雾深深不知在何处。"
        ),
        Poem(
            title = "枫桥夜泊", author = "张继",
            lines = listOf(
                line("月|yuè 落|luò 乌|wū 啼|tí 霜|shuāng 满|mǎn 天|tiān ，|_"),
                line("江|jiāng 枫|fēng 渔|yú 火|huǒ 对|duì 愁|chóu 眠|mián 。|_"),
                line("姑|gū 苏|sū 城|chéng 外|wài 寒|hán 山|shān 寺|sì ，|_"),
                line("夜|yè 半|bàn 钟|zhōng 声|shēng 到|dào 客|kè 船|chuán 。|_")
            ),
            bgEmoji = "🛶", startColor = 0xFF37474F.toInt(), endColor = 0xFF0D1B2A.toInt(),
            meaning = "月落乌啼霜满天空，江枫渔火伴着我忧愁难眠。姑苏城外的寒山寺，夜半钟声传到了客船上。"
        ),
        Poem(
            title = "别董大", author = "高适",
            lines = listOf(
                line("千|qiān 里|lǐ 黄|huáng 云|yún 白|bái 日|xūn 曛|xūn ，|_"),
                line("北|běi 风|fēng 吹|chuī 雁|yàn 雪|xuě 纷|fēn 纷|fēn 。|_"),
                line("莫|mò 愁|chóu 前|qián 路|lù 无|wú 知|zhī 己|jǐ ，|_"),
                line("天|tiān 下|xià 谁|shéi 人|rén 不|bù 识|shí 君|jūn 。|_")
            ),
            bgEmoji = "🌫️", startColor = 0xFFBCAAA4.toInt(), endColor = 0xFF5D4037.toInt(),
            meaning = "千里黄云遮住太阳天色昏暗，北风吹雁大雪纷纷。不要担心前路没有知己，天下谁不认识你呢！"
        ),
        Poem(
            title = "早发白帝城", author = "李白",
            lines = listOf(
                line("朝|zhāo 辞|cí 白|bái 帝|dì 彩|cǎi 云|yún 间|jiān ，|_"),
                line("千|qiān 里|lǐ 江|jiāng 陵|líng 一|yī 日|rì 还|huán 。|_"),
                line("两|liǎng 岸|àn 猿|yuán 声|shēng 啼|tí 不|bú 住|zhù ，|_"),
                line("轻|qīng 舟|zhōu 已|yǐ 过|guò 万|wàn 重|chóng 山|shān 。|_")
            ),
            bgEmoji = "🚣", startColor = 0xFFFF8A65.toInt(), endColor = 0xFF7B1FA2.toInt(),
            meaning = "清晨告别彩云间的白帝城，千里江陵一天就能返回。两岸猿声还在耳边回响，轻快的小船已越过万重山。"
        ),
        Poem(
            title = "晓出净慈寺送林子方", author = "杨万里",
            lines = listOf(
                line("毕|bì 竟|jìng 西|xī 湖|hú 六|liù 月|yuè 中|zhōng ，|_"),
                line("风|fēng 光|guāng 不|bù 与|yǔ 四|sì 时|shí 同|tóng 。|_"),
                line("接|jiē 天|tiān 莲|lián 叶|yè 无|wú 穷|qióng 碧|bì ，|_"),
                line("映|yìng 日|rì 荷|hé 花|huā 别|bié 样|yàng 红|hóng 。|_")
            ),
            bgEmoji = "🍀", startColor = 0xFF43A047.toInt(), endColor = 0xFF1565C0.toInt(),
            meaning = "毕竟西湖六月的风光，和别的季节不同。接天连地的莲叶碧绿无边，映着朝阳的荷花格外红艳。"
        ),
        Poem(
            title = "惠崇春江晚景", author = "苏轼",
            lines = listOf(
                line("竹|zhú 外|wài 桃|táo 花|huā 三|sān 两|liǎng 枝|zhī ，|_"),
                line("春|chūn 江|jiāng 水|shuǐ 暖|nuǎn 鸭|yā 先|xiān 知|zhī 。|_"),
                line("蒌|lóu 蒿|hāo 满|mǎn 地|dì 芦|lú 芽|yá 短|duǎn ，|_"),
                line("正|zhèng 是|shì 河|hé 豚|tún 欲|yù 上|shàng 时|shí 。|_")
            ),
            bgEmoji = "🦆", startColor = 0xFF4DB6AC.toInt(), endColor = 0xFF1B5E20.toInt(),
            meaning = "竹林外桃花开了三两枝，春江水暖鸭子最先知道。满地蒌蒿芦芽正短，正是河豚要浮上水面的时候。"
        ),
        Poem(
            title = "春日", author = "朱熹",
            lines = listOf(
                line("胜|shèng 日|rì 寻|xún 芳|fāng 泗|sì 水|shuǐ 滨|bīn ，|_"),
                line("无|wú 边|biān 光|guāng 景|jǐng 一|yī 时|shí 新|xīn 。|_"),
                line("等|děng 闲|xián 识|shí 得|dé 东|dōng 风|fēng 面|miàn ，|_"),
                line("万|wàn 紫|zǐ 千|qiān 红|hóng 总|zǒng 是|shì 春|chūn 。|_")
            ),
            bgEmoji = "☀️", startColor = 0xFFFFD54F.toInt(), endColor = 0xFFF57F17.toInt(),
            meaning = "春光明媚到泗水边赏景，无边风光一时焕然一新。随意便认出了东风的模样，万紫千红都是春天。"
        ),
        Poem(
            title = "泊船瓜洲", author = "王安石",
            lines = listOf(
                line("京|jīng 口|kǒu 瓜|guā 洲|zhōu 一|yī 水|shuǐ 间|jiān ，|_"),
                line("钟|zhōng 山|shān 只|zhǐ 隔|gé 数|shù 重|chóng 山|shān 。|_"),
                line("春|chūn 风|fēng 又|yòu 绿|lǜ 江|jiāng 南|nán 岸|àn ，|_"),
                line("明|míng 月|yuè 何|hé 时|shí 照|zhào 我|wǒ 还|huán 。|_")
            ),
            bgEmoji = "🌉", startColor = 0xFF5C9CE6.toInt(), endColor = 0xFF1A3A5C.toInt(),
            meaning = "京口和瓜洲只隔一江水，钟山也只隔着几重山。春风又吹绿了江南两岸，明月什么时候照我回家乡呢？"
        ),
        Poem(
            title = "游园不值", author = "叶绍翁",
            lines = listOf(
                line("应|yīng 怜|lián 屐|jī 齿|chǐ 印|yìn 苍|cāng 苔|tái ，|_"),
                line("小|xiǎo 扣|kòu 柴|chái 扉|fēi 久|jiǔ 不|bù 开|kāi 。|_"),
                line("春|chūn 色|sè 满|mǎn 园|yuán 关|guān 不|bú 住|zhù ，|_"),
                line("一|yī 枝|zhī 红|hóng 杏|xìng 出|chū 墙|qiáng 来|lái 。|_")
            ),
            bgEmoji = "🏡", startColor = 0xFF7CB342.toInt(), endColor = 0xFF33691E.toInt(),
            meaning = "主人大概爱惜青苔怕我踩出脚印，轻轻敲门许久没人开。满园春色是关不住的，一枝红杏探出墙来。"
        ),
        Poem(
            title = "六月二十七日望湖楼醉书", author = "苏轼",
            lines = listOf(
                line("黑|hēi 云|yún 翻|fān 墨|mò 未|wèi 遮|zhē 山|shān ，|_"),
                line("白|bái 雨|yǔ 跳|tiào 珠|zhū 乱|luàn 入|rù 船|chuán 。|_"),
                line("卷|juǎn 地|dì 风|fēng 来|lái 忽|hū 吹|chuī 散|sàn ，|_"),
                line("望|wàng 湖|hú 楼|lóu 下|xià 水|shuǐ 如|rú 天|tiān 。|_")
            ),
            bgEmoji = "⛈️", startColor = 0xFF546E7A.toInt(), endColor = 0xFF1A237E.toInt(),
            meaning = "黑云像翻墨一样压来还没遮住山，白亮的雨珠乱跳进船里。忽然卷地风来吹散乌云，望湖楼下水天一色。"
        ),
        Poem(
            title = "浪淘沙", author = "刘禹锡",
            lines = listOf(
                line("九|jiǔ 曲|qū 黄|huáng 河|hé 万|wàn 里|lǐ 沙|shā ，|_"),
                line("浪|làng 淘|táo 风|fēng 簸|bǒ 自|zì 天|tiān 涯|yá 。|_"),
                line("如|rú 今|jīn 直|zhí 上|shàng 银|yín 河|hé 去|qù ，|_"),
                line("同|tóng 到|dào 牵|qiān 牛|niú 织|zhī 女|nǚ 家|jiā 。|_")
            ),
            bgEmoji = "🌌", startColor = 0xFF5C6BC0.toInt(), endColor = 0xFF1A237E.toInt(),
            meaning = "九曲黄河夹着万里黄沙，浪淘风簸来自天涯。如今直上银河去，一起到牛郎织女家做客。"
        ),
        Poem(
            title = "江南春", author = "杜牧",
            lines = listOf(
                line("千|qiān 里|lǐ 莺|yīng 啼|tí 绿|lǜ 映|yìng 红|hóng ，|_"),
                line("水|shuǐ 村|cūn 山|shān 郭|guō 酒|jiǔ 旗|qí 风|fēng 。|_"),
                line("南|nán 朝|cháo 四|sì 百|bǎi 八|bā 十|shí 寺|sì ，|_"),
                line("多|duō 少|shǎo 楼|lóu 台|tái 烟|yān 雨|yǔ 中|zhōng 。|_")
            ),
            bgEmoji = "🏞️", startColor = 0xFF26A69A.toInt(), endColor = 0xFF37474F.toInt(),
            meaning = "千里江南莺啼绿树映着红花，水村山城酒旗迎风招展。南朝留下了四百八十座寺庙，多少楼台笼罩在烟雨中。"
        ),
        Poem(
            title = "四时田园杂兴（其三十一）", author = "范成大",
            lines = listOf(
                line("昼|zhòu 出|chū 耘|yún 田|tián 夜|yè 绩|jì 麻|má ，|_"),
                line("村|cūn 庄|zhuāng 儿|ér 女|nǚ 各|gè 当|dāng 家|jiā 。|_"),
                line("童|tóng 孙|sūn 未|wèi 解|jiě 供|gòng 耕|gēng 织|zhī ，|_"),
                line("也|yě 傍|bàng 桑|sāng 阴|yīn 学|xué 种|zhòng 瓜|guā 。|_")
            ),
            bgEmoji = "🌽", startColor = 0xFF8BC34A.toInt(), endColor = 0xFF33691E.toInt(),
            meaning = "白天锄地下田夜晚纺麻，村里的男女各自当家。小孩子还不懂耕田织布，也在桑树下学着种瓜。"
        ),

        // ==================== 新增初中古诗 ====================
        Poem(
            title = "登飞来峰", author = "王安石",
            lines = listOf(
                line("飞|fēi 来|lái 山|shān 上|shàng 千|qiān 寻|xún 塔|tǎ ，|_"),
                line("闻|wén 说|shuō 鸡|jī 鸣|míng 见|xiàn 日|rì 升|shēng 。|_"),
                line("不|bú 畏|wèi 浮|fú 云|yún 遮|zhē 望|wàng 眼|yǎn ，|_"),
                line("自|zì 缘|yuán 身|shēn 在|zài 最|zuì 高|gāo 层|céng 。|_")
            ),
            bgEmoji = "🗼", startColor = 0xFFFFB74D.toInt(), endColor = 0xFFE65100.toInt(),
            meaning = "飞来峰上有一座千寻高的塔，听说鸡鸣时可以看见日出。不怕浮云遮住远望的目光，只因为自己站在最高层。"
        ),
        Poem(
            title = "望岳", author = "杜甫",
            lines = listOf(
                line("岱|dài 宗|zōng 夫|fú 如|rú 何|hé ？|_"),
                line("齐|qí 鲁|lǔ 青|qīng 未|wèi 了|liǎo 。|_"),
                line("造|zào 化|huà 钟|zhōng 神|shén 秀|xiù ，|_"),
                line("阴|yīn 阳|yáng 割|gē 昏|hūn 晓|xiǎo 。|_"),
                line("荡|dàng 胸|xiōng 生|shēng 曾|céng 云|yún ，|_"),
                line("决|jué 眦|zì 入|rù 归|guī 鸟|niǎo 。|_"),
                line("会|huì 当|dāng 凌|líng 绝|jué 顶|dǐng ，|_"),
                line("一|yī 览|lǎn 众|zhòng 山|shān 小|xiǎo 。|_")
            ),
            bgEmoji = "🏔️", startColor = 0xFF66BB6A.toInt(), endColor = 0xFF263238.toInt(),
            meaning = "泰山怎么样？它横跨齐鲁青翠连绵。大自然把神奇秀美都给了它，山南山北分出昏晓。我定要登上最高峰，一览众山小的壮景。"
        ),
        Poem(
            title = "春望", author = "杜甫",
            lines = listOf(
                line("国|guó 破|pò 山|shān 河|hé 在|zài ，|_"),
                line("城|chéng 春|chūn 草|cǎo 木|mù 深|shēn 。|_"),
                line("感|gǎn 时|shí 花|huā 溅|jiàn 泪|lèi ，|_"),
                line("恨|hèn 别|bié 鸟|niǎo 惊|jīng 心|xīn 。|_"),
                line("烽|fēng 火|huǒ 连|lián 三|sān 月|yuè ，|_"),
                line("家|jiā 书|shū 抵|dǐ 万|wàn 金|jīn 。|_"),
                line("白|bái 头|tóu 搔|sāo 更|gèng 短|duǎn ，|_"),
                line("浑|hún 欲|yù 不|bù 胜|shèng 簪|zān 。|_")
            ),
            bgEmoji = "🍂", startColor = 0xFF8D6E63.toInt(), endColor = 0xFF3E2723.toInt(),
            meaning = "国家破败山河依旧，长安城里草木深深。感伤时局花也溅泪，恨别离鸟也惊心。烽火连月，一封家书抵得上万金。"
        ),
        Poem(
            title = "钱塘湖春行", author = "白居易",
            lines = listOf(
                line("孤|gū 山|shān 寺|sì 北|běi 贾|jiǎ 亭|tíng 西|xī ，|_"),
                line("水|shuǐ 面|miàn 初|chū 平|píng 云|yún 脚|jiǎo 低|dī 。|_"),
                line("几|jǐ 处|chù 早|zǎo 莺|yīng 争|zhēng 暖|nuǎn 树|shù ，|_"),
                line("谁|shéi 家|jiā 新|xīn 燕|yàn 啄|zhuó 春|chūn 泥|ní 。|_"),
                line("乱|luàn 花|huā 渐|jiàn 欲|yù 迷|mí 人|rén 眼|yǎn ，|_"),
                line("浅|qiǎn 草|cǎo 才|cái 能|néng 没|mò 马|mǎ 蹄|tí 。|_"),
                line("最|zuì 爱|ài 湖|hú 东|dōng 行|xíng 不|bù 足|zú ，|_"),
                line("绿|lǜ 杨|yáng 阴|yīn 里|lǐ 白|bái 沙|shā 堤|dī 。|_")
            ),
            bgEmoji = "🌼", startColor = 0xFF81C784.toInt(), endColor = 0xFF00838F.toInt(),
            meaning = "孤山寺北贾公亭西，春水初平云脚低。早莺争暖树，新燕啄春泥，乱花迷人眼，浅草没过马蹄。最爱的还是湖东，绿杨阴里的白沙堤。"
        ),
        Poem(
            title = "泊秦淮", author = "杜牧",
            lines = listOf(
                line("烟|yān 笼|lóng 寒|hán 水|shuǐ 月|yuè 笼|lóng 沙|shā ，|_"),
                line("夜|yè 泊|bó 秦|qín 淮|huái 近|jìn 酒|jiǔ 家|jiā 。|_"),
                line("商|shāng 女|nǚ 不|bù 知|zhī 亡|wáng 国|guó 恨|hèn ，|_"),
                line("隔|gé 江|jiāng 犹|yóu 唱|chàng 后|hòu 庭|tíng 花|huā 。|_")
            ),
            bgEmoji = "🎶", startColor = 0xFF7E57C2.toInt(), endColor = 0xFF1A237E.toInt(),
            meaning = "烟雾笼罩寒水月光笼罩沙滩，夜晚停船在秦淮河边的酒家。歌女不知亡国之恨，隔着江水还在唱《后庭花》。"
        ),
        Poem(
            title = "过零丁洋", author = "文天祥",
            lines = listOf(
                line("辛|xīn 苦|kǔ 遭|zāo 逢|féng 起|qǐ 一|yī 经|jīng ，|_"),
                line("干|gān 戈|gē 寥|liáo 落|luò 四|sì 周|zhōu 星|xīng 。|_"),
                line("山|shān 河|hé 破|pò 碎|suì 风|fēng 飘|piāo 絮|xù ，|_"),
                line("身|shēn 世|shì 浮|fú 沉|chén 雨|yǔ 打|dǎ 萍|píng 。|_"),
                line("惶|huáng 恐|kǒng 滩|tān 头|tóu 说|shuō 惶|huáng 恐|kǒng ，|_"),
                line("零|líng 丁|dīng 洋|yáng 里|lǐ 叹|tàn 零|líng 丁|dīng 。|_"),
                line("人|rén 生|shēng 自|zì 古|gǔ 谁|shéi 无|wú 死|sǐ ，|_"),
                line("留|liú 取|qǔ 丹|dān 心|xīn 照|zhào 汗|hàn 青|qīng 。|_")
            ),
            bgEmoji = "🌊", startColor = 0xFF546E7A.toInt(), endColor = 0xFF0D1B2A.toInt(),
            meaning = "辛苦一生遭逢乱世，四年抗敌干戈寥落。山河破碎像风中飘絮，身世浮沉如雨中浮萍。人生自古谁无死，留下一片丹心照史册。"
        ),
        Poem(
            title = "夜雨寄北", author = "李商隐",
            lines = listOf(
                line("君|jūn 问|wèn 归|guī 期|qī 未|wèi 有|yǒu 期|qī ，|_"),
                line("巴|bā 山|shān 夜|yè 雨|yǔ 涨|zhǎng 秋|qiū 池|chí 。|_"),
                line("何|hé 当|dāng 共|gòng 剪|jiǎn 西|xī 窗|chuāng 烛|zhú ，|_"),
                line("却|què 话|huà 巴|bā 山|shān 夜|yè 雨|yǔ 时|shí 。|_")
            ),
            bgEmoji = "🕯️", startColor = 0xFF6D4C41.toInt(), endColor = 0xFF1B1B1B.toInt(),
            meaning = "你问我归期还没定，巴山夜雨涨满了秋池。什么时候共剪西窗烛，再话今夜巴山夜雨的情景呢？"
        ),
        Poem(
            title = "使至塞上", author = "王维",
            lines = listOf(
                line("单|dān 车|chē 欲|yù 问|wèn 边|biān ，|_"),
                line("属|shǔ 国|guó 过|guò 居|jū 延|yán 。|_"),
                line("征|zhēng 蓬|péng 出|chū 汉|hàn 塞|sài ，|_"),
                line("归|guī 雁|yàn 入|rù 胡|hú 天|tiān 。|_"),
                line("大|dà 漠|mò 孤|gū 烟|yān 直|zhí ，|_"),
                line("长|cháng 河|hé 落|luò 日|rì 圆|yuán 。|_"),
                line("萧|xiāo 关|guān 逢|féng 候|hòu 骑|qí ，|_"),
                line("都|dū 护|hù 在|zài 燕|yān 然|rán 。|_")
            ),
            bgEmoji = "🌅", startColor = 0xFFFF8A65.toInt(), endColor = 0xFFB71C1C.toInt(),
            meaning = "轻车简从出使边塞，经过属国到达居延。像蓬草一样飘出汉塞，像归雁一样飞入胡天。大漠孤烟直，长河落日圆。"
        )
    )
}
