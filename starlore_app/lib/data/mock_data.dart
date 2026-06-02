import '../models/zodiac_model.dart';

/// 模拟数据
class MockData {
  MockData._();

  // 当前星座
  static const Zodiac currentZodiac = Zodiac(
    name: 'LEO',
    symbol: '♌',
    dateRange: '7.23 - 8.22 · 狮子座',
    emoji: '♌',
    score: 92,
  );

  // 运势数据
  static const List<Fortune> fortunes = [
    Fortune(icon: '💕', label: '爱情', value: '极佳'),
    Fortune(icon: '💼', label: '事业', value: '上升'),
    Fortune(icon: '💰', label: '财运', value: '平稳', isWarning: true),
    Fortune(icon: '🏥', label: '健康', value: '良好'),
    Fortune(icon: '🍀', label: '幸运色', value: '琥珀金'),
    Fortune(icon: '🔢', label: '幸运数', value: '7'),
  ];

  // 明星数据
  static const List<Celebrity> celebrities = [
    Celebrity(
      name: '张艺兴',
      zodiac: '天蝎座',
      description: '运势上升中',
      emoji: '🦁',
      score: 9.8,
      rank: 1,
    ),
    Celebrity(
      name: '赵丽颖',
      zodiac: '处女座',
      description: '贵人运旺',
      emoji: '👑',
      score: 9.6,
      rank: 2,
    ),
    Celebrity(
      name: '王一博',
      zodiac: '射手座',
      description: '创造力爆发',
      emoji: '🏹',
      score: 9.4,
      rank: 3,
    ),
  ];

  // 分类数据
  static const List<Category> categories = [
    Category(name: '十二星座', icon: '♈', count: '12 个星座', colorType: 'sage'),
    Category(name: '月相运势', icon: '🌙', count: '月历追踪', colorType: 'terracotta'),
    Category(name: '塔罗占卜', icon: '🔮', count: '22 张大阿卡纳', colorType: 'lavender'),
    Category(name: '星盘解析', icon: '⭐', count: '个人星盘', colorType: 'sand'),
    Category(name: '星座配对', icon: '💫', count: '缘分测试', colorType: 'terracotta'),
    Category(name: '水晶能量', icon: '🌿', count: '疗愈指南', colorType: 'sage'),
  ];

  // 聊天初始消息
  static const List<ChatMessage> initialMessages = [
    ChatMessage(
      text: '你好，星语者 ✨\n\n我是你的星语Oracle，随时准备为你解读星辰的密语。\n\n今晚的星象很特别，有什么想聊的吗？',
      isUser: false,
      time: '21:42',
    ),
    ChatMessage(
      text: '告诉我关于天蝎座今天的运势',
      isUser: true,
      time: '21:43',
    ),
    ChatMessage(
      text: '今天冥王星与金星形成和谐相位，天蝎座的你将感受到：\n\n💕 爱情运势\n单身者可能遇到令人心动的对象，已有伴侣者感情升温。\n\n💼 事业建议\n直觉敏锐，适合做重要决策。但要避免过于强势。\n\n记住：今晚月亮在你的守护宫，是冥想的好时机 🌙',
      isUser: false,
      time: '21:44',
    ),
  ];

  // AI回复模板
  static const List<String> aiResponses = [
    '让我为你解读一下... ✨\n\n根据今晚的星象，这是一个充满可能性的时刻。保持开放的心态，好运正在靠近你。',
    '星光指引着你... 🌟\n\n土星正在你的事业宫运行，这是沉淀和积累的好时机。耐心等待，收获即将到来。',
    '宇宙在低语... 🔮\n\n今天的满月能量很强，适合做冥想和反思。放下过去的执念，新的篇章即将展开。',
    '星辰为你闪耀... ✦\n\n金星与木星的相位带来幸运，社交运极佳。把握机会，可能会遇到重要的贵人。',
  ];
}
