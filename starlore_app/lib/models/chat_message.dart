/// AI 聊天消息
class ChatMessage {
  final String role; // 'user' | 'assistant' | 'system'
  final String content;
  final DateTime createdAt;
  final bool isStreaming;
  final String? agentTrace;

  ChatMessage({
    required this.role,
    required this.content,
    DateTime? createdAt,
    this.isStreaming = false,
    this.agentTrace,
  }) : createdAt = createdAt ?? DateTime.now();

  bool get isUser => role == 'user';
  bool get isAssistant => role == 'assistant';

  ChatMessage copyWith({
    String? content,
    bool? isStreaming,
    String? agentTrace,
  }) {
    return ChatMessage(
      role: role,
      content: content ?? this.content,
      createdAt: createdAt,
      isStreaming: isStreaming ?? this.isStreaming,
      agentTrace: agentTrace ?? this.agentTrace,
    );
  }

  factory ChatMessage.fromJson(Map<String, dynamic> json) {
    return ChatMessage(
      role: json['role'] ?? 'user',
      content: json['content'] ?? '',
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
      agentTrace: json['agentTrace'],
    );
  }
}
