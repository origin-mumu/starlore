import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../models/zodiac_model.dart';

/// 聊天气泡
class ChatBubble extends StatelessWidget {
  final ChatMessage message;

  const ChatBubble({super.key, required this.message});

  @override
  Widget build(BuildContext context) {
    final isUser = message.isUser;

    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: BoxConstraints(
          maxWidth: MediaQuery.of(context).size.width * 0.75,
        ),
        margin: const EdgeInsets.only(bottom: 16),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.end,
          mainAxisAlignment:
              isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
          children: [
            if (!isUser) ...[
              // AI头像
              Container(
                width: 32,
                height: 32,
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  color: AppTheme.accentSage,
                ),
                child: const Center(
                  child: Text('✦', style: TextStyle(fontSize: 14, color: AppTheme.white)),
                ),
              ),
              const SizedBox(width: 12),
            ],

            // 气泡
            Flexible(
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
                decoration: BoxDecoration(
                  color: isUser
                      ? AppTheme.accentSage
                      : AppTheme.bgCard,
                  borderRadius: BorderRadius.only(
                    topLeft: const Radius.circular(20),
                    topRight: const Radius.circular(20),
                    bottomLeft: Radius.circular(isUser ? 20 : 4),
                    bottomRight: Radius.circular(isUser ? 4 : 20),
                  ),
                  boxShadow: isUser
                      ? null
                      : [
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.04),
                            blurRadius: 8,
                            offset: const Offset(0, 2),
                          ),
                        ],
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      message.text,
                      style: TextStyle(
                        fontSize: 14,
                        height: 1.6,
                        color: isUser ? AppTheme.white : AppTheme.textDark,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      message.time,
                      style: TextStyle(
                        fontSize: 11,
                        color: isUser
                            ? AppTheme.white.withValues(alpha: 0.7)
                            : AppTheme.textLight,
                      ),
                    ),
                  ],
                ),
              ),
            ),

            if (isUser) ...[
              const SizedBox(width: 12),
              // 用户头像
              const CircleAvatar(
                radius: 16,
                backgroundColor: AppTheme.accentTerracotta,
                child: Text('🌟', style: TextStyle(fontSize: 14)),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
