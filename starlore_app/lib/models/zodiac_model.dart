/// 星座数据模型
class Zodiac {
  final String name;
  final String symbol;
  final String dateRange;
  final String emoji;
  final int score;

  const Zodiac({
    required this.name,
    required this.symbol,
    required this.dateRange,
    required this.emoji,
    this.score = 0,
  });
}

/// 运势数据模型
class Fortune {
  final String icon;
  final String label;
  final String value;
  final bool isWarning;

  const Fortune({
    required this.icon,
    required this.label,
    required this.value,
    this.isWarning = false,
  });
}

/// 明星数据模型
class Celebrity {
  final String name;
  final String zodiac;
  final String description;
  final String emoji;
  final double score;
  final int rank;

  const Celebrity({
    required this.name,
    required this.zodiac,
    required this.description,
    required this.emoji,
    required this.score,
    required this.rank,
  });
}

/// 分类数据模型
class Category {
  final String name;
  final String icon;
  final String count;
  final String colorType; // sage, terracotta, lavender, sand

  const Category({
    required this.name,
    required this.icon,
    required this.count,
    required this.colorType,
  });
}

/// 聊天消息模型
class ChatMessage {
  final String text;
  final bool isUser;
  final String time;

  const ChatMessage({
    required this.text,
    required this.isUser,
    required this.time,
  });
}
