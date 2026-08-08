package com.example.firstapplication.ui.kids

/**
 * 英语乐园数据源：26 个字母（每个含 2 个示例单词）、15 类常用单词（330 词）、60 条简单句子、30 天打卡计划
 * - ALPHABET：字母表（大写/小写/音标/示例单词+图标）
 * - CATEGORIES：单词分类（页面"单词句子"按分类学习）
 * - ALL_WORDS：全部单词（前 300 词按每 10 词一组划分 30 天打卡）
 * - SENTENCES：60 条基础句（打卡每天 2 条 + 页面句子学习）
 */
object EnglishLibrary {

    data class EnglishWord(
        val word: String,
        val phonetic: String,
        val chinese: String,
        val emoji: String
    )

    data class WordCategory(val name: String, val words: List<EnglishWord>)

    data class LetterInfo(
        val letter: String,     // 大写
        val lowercase: String,  // 小写
        val phonetic: String,   // 字母音标
        val words: List<WordSample>  // 1-2 个示例单词
    )

    data class WordSample(val word: String, val chinese: String, val emoji: String)

    data class Sentence(val en: String, val cn: String)

    data class CheckInDay(
        val theme: String,
        val words: List<EnglishWord>,
        val sentences: List<Sentence>
    )

    // ==================== 字母表（26 个） ====================

    val ALPHABET: List<LetterInfo> = listOf(
        LetterInfo("A", "a", "/eɪ/", listOf(WordSample("apple", "苹果", "🍎"), WordSample("ant", "蚂蚁", "🐜"))),
        LetterInfo("B", "b", "/biː/", listOf(WordSample("ball", "球", "⚽"), WordSample("bird", "小鸟", "🐦"))),
        LetterInfo("C", "c", "/siː/", listOf(WordSample("cat", "猫", "🐱"), WordSample("cake", "蛋糕", "🍰"))),
        LetterInfo("D", "d", "/diː/", listOf(WordSample("dog", "狗", "🐶"), WordSample("duck", "鸭子", "🦆"))),
        LetterInfo("E", "e", "/iː/", listOf(WordSample("egg", "鸡蛋", "🥚"), WordSample("elephant", "大象", "🐘"))),
        LetterInfo("F", "f", "/ef/", listOf(WordSample("fish", "鱼", "🐟"), WordSample("flower", "花", "🌸"))),
        LetterInfo("G", "g", "/dʒiː/", listOf(WordSample("grape", "葡萄", "🍇"), WordSample("goat", "山羊", "🐐"))),
        LetterInfo("H", "h", "/eɪtʃ/", listOf(WordSample("hat", "帽子", "👒"), WordSample("horse", "马", "🐴"))),
        LetterInfo("I", "i", "/aɪ/", listOf(WordSample("ice", "冰块", "🧊"), WordSample("island", "岛屿", "🏝️"))),
        LetterInfo("J", "j", "/dʒeɪ/", listOf(WordSample("juice", "果汁", "🧃"), WordSample("jump", "跳", "🦘"))),
        LetterInfo("K", "k", "/keɪ/", listOf(WordSample("kite", "风筝", "🪁"), WordSample("key", "钥匙", "🔑"))),
        LetterInfo("L", "l", "/el/", listOf(WordSample("lion", "狮子", "🦁"), WordSample("lemon", "柠檬", "🍋"))),
        LetterInfo("M", "m", "/em/", listOf(WordSample("moon", "月亮", "🌙"), WordSample("milk", "牛奶", "🥛"))),
        LetterInfo("N", "n", "/en/", listOf(WordSample("nose", "鼻子", "👃"), WordSample("nut", "坚果", "🥜"))),
        LetterInfo("O", "o", "/əʊ/", listOf(WordSample("orange", "橙子", "🍊"), WordSample("owl", "猫头鹰", "🦉"))),
        LetterInfo("P", "p", "/piː/", listOf(WordSample("pig", "小猪", "🐷"), WordSample("panda", "熊猫", "🐼"))),
        LetterInfo("Q", "q", "/kjuː/", listOf(WordSample("queen", "女王", "👸"), WordSample("question", "问题", "❓"))),
        LetterInfo("R", "r", "/ɑːr/", listOf(WordSample("rabbit", "兔子", "🐰"), WordSample("rainbow", "彩虹", "🌈"))),
        LetterInfo("S", "s", "/es/", listOf(WordSample("sun", "太阳", "☀️"), WordSample("star", "星星", "⭐"))),
        LetterInfo("T", "t", "/tiː/", listOf(WordSample("tree", "树", "🌳"), WordSample("tiger", "老虎", "🐯"))),
        LetterInfo("U", "u", "/juː/", listOf(WordSample("umbrella", "雨伞", "☂️"), WordSample("uncle", "叔叔", "👨"))),
        LetterInfo("V", "v", "/viː/", listOf(WordSample("violin", "小提琴", "🎻"), WordSample("van", "面包车", "🚐"))),
        LetterInfo("W", "w", "/ˈdʌbljuː/", listOf(WordSample("water", "水", "💧"), WordSample("whale", "鲸鱼", "🐳"))),
        LetterInfo("X", "x", "/eks/", listOf(WordSample("xylophone", "木琴", "🎼"), WordSample("fox", "狐狸", "🦊"))),
        LetterInfo("Y", "y", "/waɪ/", listOf(WordSample("yellow", "黄色", "💛"), WordSample("yo-yo", "悠悠球", "🪀"))),
        LetterInfo("Z", "z", "/zed/", listOf(WordSample("zebra", "斑马", "🦓"), WordSample("zero", "零", "0️⃣")))
    )

    // ==================== 单词分类（15 类 330 词，前 300 词用于 30 天打卡） ====================

    private fun w(word: String, phonetic: String, chinese: String, emoji: String) =
        EnglishWord(word, phonetic, chinese, emoji)

    val CATEGORIES: List<WordCategory> = listOf(
        // 1 动物（42）
        WordCategory("动物", listOf(
            w("cat", "/kæt/", "猫", "🐱"), w("dog", "/dɒɡ/", "狗", "🐶"),
            w("bird", "/bɜːd/", "小鸟", "🐦"), w("fish", "/fɪʃ/", "鱼", "🐟"),
            w("rabbit", "/ˈræbɪt/", "兔子", "🐰"), w("duck", "/dʌk/", "鸭子", "🦆"),
            w("pig", "/pɪɡ/", "小猪", "🐷"), w("cow", "/kaʊ/", "奶牛", "🐮"),
            w("sheep", "/ʃiːp/", "绵羊", "🐑"), w("horse", "/hɔːs/", "马", "🐴"),
            w("chicken", "/ˈtʃɪkɪn/", "小鸡", "🐔"), w("monkey", "/ˈmʌŋki/", "猴子", "🐵"),
            w("panda", "/ˈpændə/", "熊猫", "🐼"), w("bear", "/beə(r)/", "熊", "🐻"),
            w("tiger", "/ˈtaɪɡə(r)/", "老虎", "🐯"), w("lion", "/ˈlaɪən/", "狮子", "🦁"),
            w("elephant", "/ˈelɪfənt/", "大象", "🐘"), w("giraffe", "/dʒəˈrɑːf/", "长颈鹿", "🦒"),
            w("zebra", "/ˈzebrə/", "斑马", "🦓"), w("deer", "/dɪə(r)/", "小鹿", "🦌"),
            w("fox", "/fɒks/", "狐狸", "🦊"), w("wolf", "/wʊlf/", "狼", "🐺"),
            w("mouse", "/maʊs/", "老鼠", "🐭"), w("frog", "/frɒɡ/", "青蛙", "🐸"),
            w("bee", "/biː/", "蜜蜂", "🐝"), w("ant", "/ænt/", "蚂蚁", "🐜"),
            w("butterfly", "/ˈbʌtəflaɪ/", "蝴蝶", "🦋"), w("turtle", "/ˈtɜːtl/", "乌龟", "🐢"),
            w("crab", "/kræb/", "螃蟹", "🦀"), w("shark", "/ʃɑːk/", "鲨鱼", "🦈"),
            w("whale", "/weɪl/", "鲸鱼", "🐳"), w("dolphin", "/ˈdɒlfɪn/", "海豚", "🐬"),
            w("penguin", "/ˈpeŋɡwɪn/", "企鹅", "🐧"), w("owl", "/aʊl/", "猫头鹰", "🦉"),
            w("parrot", "/ˈpærət/", "鹦鹉", "🦜"), w("snake", "/sneɪk/", "蛇", "🐍"),
            w("kangaroo", "/ˌkæŋɡəˈruː/", "袋鼠", "🦘"), w("squirrel", "/ˈskwɪrəl/", "松鼠", "🐿️"),
            w("goat", "/ɡəʊt/", "山羊", "🐐"), w("turkey", "/ˈtɜːki/", "火鸡", "🦃"),
            w("camel", "/ˈkæml/", "骆驼", "🐫"), w("puppy", "/ˈpʌpi/", "小狗", "🐶")
        ).take(42)),
        // 2 水果（26）
        WordCategory("水果", listOf(
            w("apple", "/ˈæpl/", "苹果", "🍎"), w("banana", "/bəˈnɑːnə/", "香蕉", "🍌"),
            w("orange", "/ˈɒrɪndʒ/", "橙子", "🍊"), w("pear", "/peə(r)/", "梨", "🍐"),
            w("grape", "/ɡreɪp/", "葡萄", "🍇"), w("peach", "/piːtʃ/", "桃子", "🍑"),
            w("mango", "/ˈmæŋɡəʊ/", "芒果", "🥭"), w("watermelon", "/ˈwɔːtəmelən/", "西瓜", "🍉"),
            w("strawberry", "/ˈstrɔːbəri/", "草莓", "🍓"), w("cherry", "/ˈtʃeri/", "樱桃", "🍒"),
            w("lemon", "/ˈlemən/", "柠檬", "🍋"), w("pineapple", "/ˈpaɪnæpl/", "菠萝", "🍍"),
            w("kiwi", "/ˈkiːwiː/", "猕猴桃", "🥝"), w("plum", "/plʌm/", "李子", "🟣"),
            w("blueberry", "/ˈbluːbəri/", "蓝莓", "🫐"), w("coconut", "/ˈkəʊkənʌt/", "椰子", "🥥"),
            w("melon", "/ˈmelən/", "甜瓜", "🍈"), w("durian", "/ˈdʊəriən/", "榴莲", "🫒"),
            w("lychee", "/ˌlaɪˈtʃiː/", "荔枝", "🔴"), w("grapefruit", "/ˈɡreɪpfruːt/", "西柚", "🍊"),
            w("pomegranate", "/ˈpɒmɪɡrænɪt/", "石榴", "🔴"), w("persimmon", "/pəˈsɪmən/", "柿子", "🟠"),
            w("apricot", "/ˈeɪprɪkɒt/", "杏", "🟠"), w("tangerine", "/ˌtændʒəˈriːn/", "橘子", "🍊"),
            w("berry", "/ˈberi/", "浆果", "🫐"), w("fruit", "/fruːt/", "水果", "🍎")
        ).take(26)),
        // 3 蔬菜（22）
        WordCategory("蔬菜", listOf(
            w("carrot", "/ˈkærət/", "胡萝卜", "🥕"), w("potato", "/pəˈteɪtəʊ/", "土豆", "🥔"),
            w("tomato", "/təˈmɑːtəʊ/", "西红柿", "🍅"), w("onion", "/ˈʌnjən/", "洋葱", "🧅"),
            w("corn", "/kɔːn/", "玉米", "🌽"), w("cucumber", "/ˈkjuːkʌmbə(r)/", "黄瓜", "🥒"),
            w("pumpkin", "/ˈpʌmpkɪn/", "南瓜", "🎃"), w("mushroom", "/ˈmʌʃrʊm/", "蘑菇", "🍄"),
            w("pepper", "/ˈpepə(r)/", "辣椒", "🌶️"), w("broccoli", "/ˈbrɒkəli/", "西兰花", "🥦"),
            w("garlic", "/ˈɡɑːlɪk/", "大蒜", "🧄"), w("ginger", "/ˈdʒɪndʒə(r)/", "生姜", "🫚"),
            w("cabbage", "/ˈkæbɪdʒ/", "卷心菜", "🥬"), w("celery", "/ˈseləri/", "芹菜", "🥬"),
            w("lettuce", "/ˈletɪs/", "生菜", "🥬"), w("pea", "/piː/", "豌豆", "🫛"),
            w("bean", "/biːn/", "豆子", "🫘"), w("eggplant", "/ˈeɡplɑːnt/", "茄子", "🍆"),
            w("radish", "/ˈrædɪʃ/", "萝卜", "🌶️"), w("sweet potato", "/swiːt pəˈteɪtəʊ/", "红薯", "🍠"),
            w("spinach", "/ˈspɪnɪtʃ/", "菠菜", "🥬"), w("vegetable", "/ˈvedʒtəbl/", "蔬菜", "🥗")
        ).take(22)),
        // 4 食物（30）
        WordCategory("食物", listOf(
            w("rice", "/raɪs/", "米饭", "🍚"), w("noodle", "/ˈnuːdl/", "面条", "🍜"),
            w("bread", "/bred/", "面包", "🍞"), w("cake", "/keɪk/", "蛋糕", "🍰"),
            w("egg", "/eɡ/", "鸡蛋", "🥚"), w("milk", "/mɪlk/", "牛奶", "🥛"),
            w("water", "/ˈwɔːtə(r)/", "水", "💧"), w("juice", "/dʒuːs/", "果汁", "🧃"),
            w("tea", "/tiː/", "茶", "🍵"), w("coffee", "/ˈkɒfi/", "咖啡", "☕"),
            w("meat", "/miːt/", "肉", "🥩"), w("pizza", "/ˈpiːtsə/", "披萨", "🍕"),
            w("hamburger", "/ˈhæmbɜːɡə(r)/", "汉堡", "🍔"), w("candy", "/ˈkændi/", "糖果", "🍬"),
            w("ice cream", "/ˌaɪs ˈkriːm/", "冰淇淋", "🍦"), w("cookie", "/ˈkʊki/", "饼干", "🍪"),
            w("soup", "/suːp/", "汤", "🍲"), w("salad", "/ˈsæləd/", "沙拉", "🥗"),
            w("french fries", "/frentʃ fraɪz/", "薯条", "🍟"), w("sausage", "/ˈsɒsɪdʒ/", "香肠", "🌭"),
            w("sandwich", "/ˈsænwɪtʃ/", "三明治", "🥪"), w("dumpling", "/ˈdʌmplɪŋ/", "饺子", "🥟"),
            w("honey", "/ˈhʌni/", "蜂蜜", "🍯"), w("chocolate", "/ˈtʃɒklət/", "巧克力", "🍫"),
            w("butter", "/ˈbʌtə(r)/", "黄油", "🧈"), w("cheese", "/tʃiːz/", "奶酪", "🧀"),
            w("jam", "/dʒæm/", "果酱", "🍓"), w("popcorn", "/ˈpɒpkɔːn/", "爆米花", "🍿"),
            w("breakfast", "/ˈbrekfəst/", "早餐", "🥣"), w("dinner", "/ˈdɪnə(r)/", "晚餐", "🍽️")
        ).take(30)),
        // 5 颜色（14）
        WordCategory("颜色", listOf(
            w("red", "/red/", "红色", "🔴"), w("yellow", "/ˈjeləʊ/", "黄色", "🟡"),
            w("blue", "/bluː/", "蓝色", "🔵"), w("green", "/ɡriːn/", "绿色", "🟢"),
            w("orange", "/ˈɒrɪndʒ/", "橙色", "🟠"), w("purple", "/ˈpɜːpl/", "紫色", "🟣"),
            w("pink", "/pɪŋk/", "粉色", "🌸"), w("black", "/blæk/", "黑色", "⚫"),
            w("white", "/waɪt/", "白色", "⚪"), w("brown", "/braʊn/", "棕色", "🟤"),
            w("gray", "/ɡreɪ/", "灰色", "🩶"), w("gold", "/ɡəʊld/", "金色", "🟡"),
            w("silver", "/ˈsɪlvə(r)/", "银色", "⚪"), w("color", "/ˈkʌlə(r)/", "颜色", "🎨")
        ).take(14)),
        // 6 数字（14）
        WordCategory("数字", listOf(
            w("one", "/wʌn/", "一", "1️⃣"), w("two", "/tuː/", "二", "2️⃣"),
            w("three", "/θriː/", "三", "3️⃣"), w("four", "/fɔː(r)/", "四", "4️⃣"),
            w("five", "/faɪv/", "五", "5️⃣"), w("six", "/sɪks/", "六", "6️⃣"),
            w("seven", "/ˈsevn/", "七", "7️⃣"), w("eight", "/eɪt/", "八", "8️⃣"),
            w("nine", "/naɪn/", "九", "9️⃣"), w("ten", "/ten/", "十", "🔟"),
            w("zero", "/ˈzɪərəʊ/", "零", "0️⃣"), w("hundred", "/ˈhʌndrəd/", "一百", "💯"),
            w("number", "/ˈnʌmbə(r)/", "数字", "🔢"), w("count", "/kaʊnt/", "数数", "🧮")
        ).take(14)),
        // 7 家庭（24）
        WordCategory("家庭", listOf(
            w("father", "/ˈfɑːðə(r)/", "爸爸", "👨"), w("mother", "/ˈmʌðə(r)/", "妈妈", "👩"),
            w("brother", "/ˈbrʌðə(r)/", "哥哥/弟弟", "👦"), w("sister", "/ˈsɪstə(r)/", "姐姐/妹妹", "👧"),
            w("grandpa", "/ˈɡrænpɑː/", "爷爷", "👴"), w("grandma", "/ˈɡrænmɑː/", "奶奶", "👵"),
            w("uncle", "/ˈʌŋkl/", "叔叔", "👨"), w("aunt", "/ɑːnt/", "阿姨", "👩"),
            w("family", "/ˈfæməli/", "家庭", "👨‍👩‍👧‍👦"), w("home", "/həʊm/", "家", "🏠"),
            w("baby", "/ˈbeɪbi/", "宝宝", "👶"), w("parent", "/ˈpeərənt/", "父母", "🧑‍🤝‍🧑"),
            w("son", "/sʌn/", "儿子", "👦"), w("daughter", "/ˈdɔːtə(r)/", "女儿", "👧"),
            w("husband", "/ˈhʌzbənd/", "丈夫", "👨"), w("wife", "/waɪf/", "妻子", "👩"),
            w("cousin", "/ˈkʌzn/", "堂/表亲", "🧒"), w("boy", "/bɔɪ/", "男孩", "👦"),
            w("girl", "/ɡɜːl/", "女孩", "👧"), w("friend", "/frend/", "朋友", "🧑‍🤝‍🧑"),
            w("people", "/ˈpiːpl/", "人们", "👫"), w("person", "/ˈpɜːsn/", "人", "🧑"),
            w("child", "/tʃaɪld/", "孩子", "🧒"), w("kid", "/kɪd/", "小孩", "👶")
        ).take(24)),
        // 8 身体（24）
        WordCategory("身体", listOf(
            w("head", "/hed/", "头", "👤"), w("eye", "/aɪ/", "眼睛", "👁️"),
            w("ear", "/ɪə(r)/", "耳朵", "👂"), w("nose", "/nəʊz/", "鼻子", "👃"),
            w("mouth", "/maʊθ/", "嘴巴", "👄"), w("hand", "/hænd/", "手", "✋"),
            w("foot", "/fʊt/", "脚", "🦶"), w("leg", "/leɡ/", "腿", "🦵"),
            w("arm", "/ɑːm/", "手臂", "💪"), w("face", "/feɪs/", "脸", "🙂"),
            w("hair", "/heə(r)/", "头发", "💇"), w("tooth", "/tuːθ/", "牙齿", "🦷"),
            w("shoulder", "/ˈʃəʊldə(r)/", "肩膀", "🤷"), w("knee", "/niː/", "膝盖", "🦵"),
            w("finger", "/ˈfɪŋɡə(r)/", "手指", "👉"), w("neck", "/nek/", "脖子", "🦒"),
            w("back", "/bæk/", "背", "🧍"), w("belly", "/ˈbeli/", "肚子", "🤰"),
            w("tongue", "/tʌŋ/", "舌头", "👅"), w("eyebrow", "/ˈaɪbraʊ/", "眉毛", "🧑"),
            w("body", "/ˈbɒdi/", "身体", "🧍"), w("brain", "/breɪn/", "大脑", "🧠"),
            w("heart", "/hɑːt/", "心脏", "❤️"), w("bone", "/bəʊn/", "骨头", "🦴")
        ).take(24)),
        // 9 动作（24）
        WordCategory("动作", listOf(
            w("run", "/rʌn/", "跑", "🏃"), w("jump", "/dʒʌmp/", "跳", "🦘"),
            w("walk", "/wɔːk/", "走", "🚶"), w("sit", "/sɪt/", "坐", "🪑"),
            w("stand", "/stænd/", "站", "🧍"), w("swim", "/swɪm/", "游泳", "🏊"),
            w("fly", "/flaɪ/", "飞", "✈️"), w("dance", "/dɑːns/", "跳舞", "💃"),
            w("sing", "/sɪŋ/", "唱歌", "🎤"), w("read", "/riːd/", "读", "📖"),
            w("write", "/raɪt/", "写", "✍️"), w("draw", "/drɔː/", "画", "🎨"),
            w("eat", "/iːt/", "吃", "🍽️"), w("drink", "/drɪŋk/", "喝", "🥤"),
            w("sleep", "/sliːp/", "睡觉", "😴"), w("play", "/pleɪ/", "玩", "🎮"),
            w("laugh", "/lɑːf/", "笑", "😂"), w("cry", "/kraɪ/", "哭", "😢"),
            w("clap", "/klæp/", "拍手", "👏"), w("open", "/ˈəʊpən/", "打开", "🔓"),
            w("close", "/kləʊz/", "关闭", "🔒"), w("clean", "/kliːn/", "打扫", "🧹"),
            w("wash", "/wɒʃ/", "洗", "🧼"), w("smile", "/smaɪl/", "微笑", "😊")
        ).take(24)),
        // 10 天气与自然（24）
        WordCategory("天气自然", listOf(
            w("sun", "/sʌn/", "太阳", "☀️"), w("rain", "/reɪn/", "雨", "🌧️"),
            w("cloud", "/klaʊd/", "云", "☁️"), w("wind", "/wɪnd/", "风", "💨"),
            w("snow", "/snəʊ/", "雪", "❄️"), w("storm", "/stɔːm/", "暴风雨", "⛈️"),
            w("rainbow", "/ˈreɪnbəʊ/", "彩虹", "🌈"), w("sky", "/skaɪ/", "天空", "🌌"),
            w("moon", "/muːn/", "月亮", "🌙"), w("star", "/stɑː(r)/", "星星", "⭐"),
            w("weather", "/ˈweðə(r)/", "天气", "🌤️"), w("hot", "/hɒt/", "热", "🔥"),
            w("cold", "/kəʊld/", "冷", "🧊"), w("warm", "/wɔːm/", "温暖", "🌞"),
            w("cool", "/kuːl/", "凉爽", "🍃"), w("tree", "/triː/", "树", "🌳"),
            w("flower", "/ˈflaʊə(r)/", "花", "🌸"), w("grass", "/ɡrɑːs/", "草", "🌱"),
            w("mountain", "/ˈmaʊntən/", "山", "⛰️"), w("river", "/ˈrɪvə(r)/", "河流", "🏞️"),
            w("sea", "/siː/", "大海", "🌊"), w("beach", "/biːtʃ/", "沙滩", "🏖️"),
            w("forest", "/ˈfɒrɪst/", "森林", "🌲"), w("nature", "/ˈneɪtʃə(r)/", "自然", "🌍")
        ).take(24)),
        // 11 学校（24）
        WordCategory("学校", listOf(
            w("school", "/skuːl/", "学校", "🏫"), w("book", "/bʊk/", "书", "📚"),
            w("pen", "/pen/", "钢笔", "🖊️"), w("pencil", "/ˈpensl/", "铅笔", "✏️"),
            w("bag", "/bæɡ/", "书包", "🎒"), w("desk", "/desk/", "课桌", "🪑"),
            w("chair", "/tʃeə(r)/", "椅子", "🪑"), w("teacher", "/ˈtiːtʃə(r)/", "老师", "👩‍🏫"),
            w("student", "/ˈstjuːdnt/", "学生", "🧑‍🎓"), w("class", "/klɑːs/", "班级", "🏫"),
            w("lesson", "/ˈlesn/", "课程", "📖"), w("blackboard", "/ˈblækbɔːd/", "黑板", "⬛"),
            w("ruler", "/ˈruːlə(r)/", "尺子", "📏"), w("eraser", "/ɪˈreɪzə(r)/", "橡皮", "🧽"),
            w("crayon", "/ˈkreɪən/", "蜡笔", "🖍️"), w("paper", "/ˈpeɪpə(r)/", "纸", "📄"),
            w("library", "/ˈlaɪbrəri/", "图书馆", "🏛️"), w("homework", "/ˈhəʊmwɜːk/", "作业", "📝"),
            w("exam", "/ɪɡˈzæm/", "考试", "📋"), w("answer", "/ˈɑːnsə(r)/", "答案", "✅"),
            w("question", "/ˈkwestʃən/", "问题", "❓"), w("word", "/wɜːd/", "单词", "🔤"),
            w("letter", "/ˈletə(r)/", "字母", "🔠"), w("song", "/sɒŋ/", "歌曲", "🎵")
        ).take(24)),
        // 12 交通（20）
        WordCategory("交通", listOf(
            w("car", "/kɑː(r)/", "小汽车", "🚗"), w("bus", "/bʌs/", "公交车", "🚌"),
            w("bike", "/baɪk/", "自行车", "🚲"), w("train", "/treɪn/", "火车", "🚂"),
            w("plane", "/pleɪn/", "飞机", "✈️"), w("ship", "/ʃɪp/", "轮船", "🚢"),
            w("taxi", "/ˈtæksi/", "出租车", "🚕"), w("truck", "/trʌk/", "卡车", "🚚"),
            w("motorcycle", "/ˈməʊtəsaɪkl/", "摩托车", "🏍️"), w("boat", "/bəʊt/", "小船", "⛵"),
            w("subway", "/ˈsʌbweɪ/", "地铁", "🚇"), w("helicopter", "/ˈhelɪkɒptə(r)/", "直升机", "🚁"),
            w("rocket", "/ˈrɒkɪt/", "火箭", "🚀"), w("van", "/væn/", "面包车", "🚐"),
            w("jeep", "/dʒiːp/", "吉普车", "🚙"), w("scooter", "/ˈskuːtə(r)/", "滑板车", "🛴"),
            w("trolley", "/ˈtrɒli/", "电车", "🚊"), w("ambulance", "/ˈæmbjələns/", "救护车", "🚑"),
            w("police car", "/pəˈliːs kɑː(r)/", "警车", "🚓"), w("fire truck", "/ˈfaɪə trʌk/", "消防车", "🚒")
        ).take(20)),
        // 13 衣物（20）
        WordCategory("衣物", listOf(
            w("shirt", "/ʃɜːt/", "衬衫", "👔"), w("skirt", "/skɜːt/", "短裙", "👗"),
            w("pants", "/pænts/", "裤子", "👖"), w("dress", "/dres/", "连衣裙", "👗"),
            w("hat", "/hæt/", "帽子", "👒"), w("cap", "/kæp/", "鸭舌帽", "🧢"),
            w("shoe", "/ʃuː/", "鞋子", "👟"), w("sock", "/sɒk/", "袜子", "🧦"),
            w("coat", "/kəʊt/", "外套", "🧥"), w("jacket", "/ˈdʒækɪt/", "夹克", "🧥"),
            w("glove", "/ɡlʌv/", "手套", "🧤"), w("scarf", "/skɑːf/", "围巾", "🧣"),
            w("sweater", "/ˈswetə(r)/", "毛衣", "🧶"), w("tshirt", "/ˈtiː ʃɜːt/", "T恤", "👕"),
            w("shorts", "/ʃɔːts/", "短裤", "🩳"), w("slippers", "/ˈslɪpəz/", "拖鞋", "🩴"),
            w("boots", "/buːts/", "靴子", "🥾"), w("umbrella", "/ʌmˈbrelə/", "雨伞", "☂️"),
            w("glasses", "/ˈɡlɑːsɪz/", "眼镜", "👓"), w("watch", "/wɒtʃ/", "手表", "⌚")
        ).take(20)),
        // 14 玩具与生活（22）
        WordCategory("玩具生活", listOf(
            w("toy", "/tɔɪ/", "玩具", "🧸"), w("ball", "/bɔːl/", "球", "⚽"),
            w("doll", "/dɒl/", "洋娃娃", "🎎"), w("kite", "/kaɪt/", "风筝", "🪁"),
            w("clock", "/klɒk/", "时钟", "🕐"), w("phone", "/fəʊn/", "手机", "📱"),
            w("key", "/kiː/", "钥匙", "🔑"), w("lamp", "/læmp/", "台灯", "💡"),
            w("mirror", "/ˈmɪrə(r)/", "镜子", "🪞"), w("cup", "/kʌp/", "杯子", "☕"),
            w("bowl", "/bəʊl/", "碗", "🥣"), w("plate", "/pleɪt/", "盘子", "🍽️"),
            w("spoon", "/spuːn/", "勺子", "🥄"), w("fork", "/fɔːk/", "叉子", "🍴"),
            w("knife", "/naɪf/", "刀", "🔪"), w("box", "/bɒks/", "盒子", "📦"),
            w("door", "/dɔː(r)/", "门", "🚪"), w("window", "/ˈwɪndəʊ/", "窗户", "🪟"),
            w("bed", "/bed/", "床", "🛏️"), w("table", "/ˈteɪbl/", "桌子", "🪵"),
            w("sofa", "/ˈsəʊfə/", "沙发", "🛋️"), w("room", "/ruːm/", "房间", "🚪")
        ).take(22)),
        // 15 职业（20）
        WordCategory("职业", listOf(
            w("doctor", "/ˈdɒktə(r)/", "医生", "👨‍⚕️"), w("nurse", "/nɜːs/", "护士", "👩‍⚕️"),
            w("police", "/pəˈliːs/", "警察", "👮"), w("cook", "/kʊk/", "厨师", "👨‍🍳"),
            w("farmer", "/ˈfɑːmə(r)/", "农民", "👨‍🌾"), w("driver", "/ˈdraɪvə(r)/", "司机", "🚗"),
            w("worker", "/ˈwɜːkə(r)/", "工人", "👷"), w("singer", "/ˈsɪŋə(r)/", "歌手", "🎤"),
            w("dancer", "/ˈdɑːnsə(r)/", "舞者", "💃"), w("painter", "/ˈpeɪntə(r)/", "画家", "🎨"),
            w("scientist", "/ˈsaɪəntɪst/", "科学家", "🔬"), w("pilot", "/ˈpaɪlət/", "飞行员", "👨‍✈️"),
            w("soldier", "/ˈsəʊldʒə(r)/", "士兵", "🪖"), w("judge", "/dʒʌdʒ/", "法官", "⚖️"),
            w("writer", "/ˈraɪtə(r)/", "作家", "✍️"), w("actor", "/ˈæktə(r)/", "演员", "🎭"),
            w("fireman", "/ˈfaɪəmən/", "消防员", "👨‍🚒"), w("postman", "/ˈpəʊstmən/", "邮递员", "📮"),
            w("barber", "/ˈbɑːbə(r)/", "理发师", "💇"), w("vet", "/vet/", "兽医", "🩺")
        ).take(20))
    )

    /** 全部单词（展平，前 300 词按 10 词/天划分 30 天打卡） */
    val ALL_WORDS: List<EnglishWord> = CATEGORIES.flatMap { it.words }

    /** 单词所属分类名（打卡主题显示用） */
    fun themeFor(index: Int): String {
        var acc = 0
        for (c in CATEGORIES) {
            acc += c.words.size
            if (index < acc) return c.name
        }
        return "单词"
    }

    // ==================== 简单句子（60 条，打卡每天 2 条） ====================

    val SENTENCES: List<Sentence> = listOf(
        Sentence("Hello! How are you?", "你好！你好吗？"),
        Sentence("I am fine, thank you.", "我很好，谢谢。"),
        Sentence("What is your name?", "你叫什么名字？"),
        Sentence("My name is Tom.", "我的名字叫汤姆。"),
        Sentence("Nice to meet you.", "很高兴见到你。"),
        Sentence("Good morning!", "早上好！"),
        Sentence("Good night!", "晚安！"),
        Sentence("I love my mom.", "我爱我的妈妈。"),
        Sentence("I love my dad.", "我爱我的爸爸。"),
        Sentence("This is my family.", "这是我的家人。"),
        Sentence("I have a little dog.", "我有一只小狗。"),
        Sentence("The cat likes milk.", "小猫喜欢牛奶。"),
        Sentence("I like apples.", "我喜欢苹果。"),
        Sentence("Do you like bananas?", "你喜欢香蕉吗？"),
        Sentence("I want some water.", "我想要一些水。"),
        Sentence("Let's eat breakfast.", "我们吃早餐吧。"),
        Sentence("I can run fast.", "我能跑得很快。"),
        Sentence("I can jump high.", "我能跳得很高。"),
        Sentence("I like to sing.", "我喜欢唱歌。"),
        Sentence("I like to dance.", "我喜欢跳舞。"),
        Sentence("Let's play a game.", "我们来玩游戏吧。"),
        Sentence("I go to school.", "我去上学。"),
        Sentence("I read a book.", "我读一本书。"),
        Sentence("I draw a picture.", "我画一幅画。"),
        Sentence("The sky is blue.", "天空是蓝色的。"),
        Sentence("The sun is bright.", "太阳很明亮。"),
        Sentence("It is raining today.", "今天下雨了。"),
        Sentence("The rainbow is beautiful.", "彩虹真美丽。"),
        Sentence("I see a big tree.", "我看到一棵大树。"),
        Sentence("The flower is red.", "这朵花是红色的。"),
        Sentence("One, two, three!", "一、二、三！"),
        Sentence("I can count to ten.", "我能数到十。"),
        Sentence("I have two hands.", "我有两只手。"),
        Sentence("I can see with my eyes.", "我用眼睛看。"),
        Sentence("I can hear with my ears.", "我用耳朵听。"),
        Sentence("I brush my teeth.", "我刷牙。"),
        Sentence("I wash my face.", "我洗脸。"),
        Sentence("I am happy today.", "我今天很开心。"),
        Sentence("I am hungry.", "我饿了。"),
        Sentence("I am thirsty.", "我渴了。"),
        Sentence("The apple is red and sweet.", "苹果又红又甜。"),
        Sentence("The banana is yellow.", "香蕉是黄色的。"),
        Sentence("I like the color blue.", "我喜欢蓝色。"),
        Sentence("My shirt is white.", "我的衬衫是白色的。"),
        Sentence("I put on my shoes.", "我穿上鞋子。"),
        Sentence("This is a big car.", "这是一辆大汽车。"),
        Sentence("The bus is coming.", "公交车来了。"),
        Sentence("I take a plane to travel.", "我坐飞机去旅行。"),
        Sentence("The train is very fast.", "火车非常快。"),
        Sentence("I help my mom clean.", "我帮妈妈打扫。"),
        Sentence("I share my toys.", "我分享我的玩具。"),
        Sentence("The dog is my friend.", "小狗是我的朋友。"),
        Sentence("I listen to my teacher.", "我听老师的话。"),
        Sentence("The doctor is kind.", "医生很和蔼。"),
        Sentence("Goodbye! See you tomorrow.", "再见！明天见。"),
        Sentence("Thank you very much.", "非常感谢你。"),
        Sentence("I am sorry.", "对不起。"),
        Sentence("Please help me.", "请帮帮我。"),
        Sentence("Have a nice day!", "祝你今天愉快！"),
        Sentence("I love learning English.", "我爱学英语。")
    )

    // ==================== 30 天打卡计划 ====================

    /** 每天 10 个单词（ALL_WORDS 前 300 词按 10 词一组）+ 2 条句子 */
    val CHECK_IN_DAYS: List<CheckInDay> = (0 until 30).map { day ->
        val start = day * 10
        CheckInDay(
            theme = themeFor(start),
            words = ALL_WORDS.subList(start, start + 10),
            sentences = SENTENCES.subList(day * 2, day * 2 + 2)
        )
    }

    // ==================== 单词例句（每个打卡单词自动配 2 句） ====================

    /** 动词类单词（用 can/let's 模板） */
    private val ACTION_WORDS = setOf(
        "run", "jump", "walk", "sit", "stand", "swim", "fly", "dance", "sing", "read",
        "write", "draw", "eat", "drink", "sleep", "play", "laugh", "cry", "clap", "open",
        "close", "clean", "wash", "smile", "count", "answer"
    )

    /** 颜色类单词 */
    private val COLOR_WORDS = setOf(
        "red", "yellow", "blue", "green", "orange", "purple", "pink",
        "black", "white", "brown", "gray", "gold", "silver"
    )

    /** 数字类单词 */
    private val NUMBER_WORDS = setOf(
        "one", "two", "three", "four", "five", "six", "seven",
        "eight", "nine", "ten", "zero", "hundred"
    )

    /** 元音开头用 an，否则用 a */
    private fun withArticle(word: String): String =
        if (word.firstOrNull()?.let { it in "aeiou" } == true) "an" else "a"

    /** 为单词生成 2 个例句（按词性选择模板，中文同步翻译） */
    fun exampleSentencesFor(word: EnglishWord): List<Sentence> {
        val w = word.word.lowercase()
        val cn = word.chinese
        return when {
            w in ACTION_WORDS -> listOf(
                Sentence("I can $w.", "我会$cn。"),
                Sentence("Let's $w!", "我们一起来${cn}吧！")
            )
            w in COLOR_WORDS -> listOf(
                Sentence("It is $w.", "它是${cn}的。"),
                Sentence("I like the color $w.", "我喜欢${cn}。")
            )
            w in NUMBER_WORDS -> listOf(
                Sentence("I can count to $w.", "我能数到$w。"),
                Sentence("Number $w is my lucky number.", "数字${w}是我的幸运数字。")
            )
            else -> listOf(
                Sentence("This is ${withArticle(w)} $w.", "这是一个$cn。"),
                Sentence("I see ${withArticle(w)} $w.", "我看见一个$cn。")
            )
        }
    }
}
