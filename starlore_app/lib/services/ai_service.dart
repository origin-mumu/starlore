import 'dart:math';
import 'api_client.dart';

/// AI 对话服务 — SSE 流式 + Mock 模式
class AiService {
  /// 流式 AI 对话（需登录）
  static Stream<String> chatStream(String message,
      {String model = 'deepseek-chat'}) {
    return ApiClient.sse(
      '/ai/multi-agent-sse?model=$model',
      body: {
        'messages': [
          {'role': 'user', 'content': message}
        ],
      },
    );
  }

  /// Mock 模式 — 本地模拟 AI 回复（游客用）
  static Stream<String> mockChatStream(String message) async* {
    final random = Random();

    // 模拟思考延迟
    await Future.delayed(Duration(milliseconds: 800 + random.nextInt(600)));

    final responses = _getMockResponses(message);
    final response = responses[random.nextInt(responses.length)];

    // 逐字输出模拟打字效果
    for (int i = 0; i < response.length; i++) {
      yield response[i];
      // 每个字符的间隔，遇到标点稍长
      final char = response[i];
      final isPunctuation = '，。！？、；：…'.contains(char);
      await Future.delayed(Duration(
        milliseconds: isPunctuation ? 80 + random.nextInt(60) : 20 + random.nextInt(30),
      ));
    }
  }

  static List<String> _getMockResponses(String input) {
    final lower = input.toLowerCase();

    if (lower.contains('星座') || lower.contains('运势')) {
      return [
        '✨ 星辰的轨迹提示着新的可能。最近的星象表明，保持开放心态会带来意想不到的机遇。不过，这只是本地演示模式哦～登录后可以获得由 DeepSeek 云端 AI 提供的深度星座解析。',
        '🌙 月亮与金星的相位暗示着情感层面的温暖变化。试着把注意力放在身边的人和事上，会有温暖的回馈。想要更精准的星座运势分析？登录后即可体验真实 AI 对话。',
      ];
    }

    if (lower.contains('你好') || lower.contains('hi') || lower.contains('hello')) {
      return [
        '你好呀 ☺️ 我是星语助手，很高兴遇见你！现在是本地演示模式，我的回复是预设的。登录后，你将和真正的 DeepSeek 云端 AI 对话，获得更有深度的交流体验 ✨',
        '嗨～欢迎来到星语世界 🌟 目前你在体验游客模式，回复是本地预设的。登录后可以和真正的 AI 助手聊天哦！',
      ];
    }

    return [
      '感谢你的提问 💫 作为星语助手，我很想给你一个深思熟虑的回答。不过现在是本地演示模式，回复内容是预设的。登录后，你将享受由 DeepSeek 驱动的真实 AI 对话，获得更丰富、更个性化的回复 ✨',
      '这是一个很好的问题 🌙 在演示模式下，我只能给出预设回复。但登录后，你可以和真正的 AI 助手进行深度对话，它会根据你的问题给出独一无二的分析和建议。',
      '谢谢你的分享 🌟 现在的回复是本地预设的演示内容。想要获得真实、智能的 AI 回复吗？只需登录就可以体验由云端 AI 驱动的完整对话功能～',
    ];
  }
}
