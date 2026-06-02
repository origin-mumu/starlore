import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../data/mock_data.dart';
import '../models/zodiac_model.dart';
import '../widgets/chat_bubble.dart';

/// AI对话页：星际通讯
class ChatPage extends StatefulWidget {
  const ChatPage({super.key});

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> {
  final TextEditingController _controller = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  late List<ChatMessage> _messages;
  int _aiResponseIndex = 0;

  @override
  void initState() {
    super.initState();
    _messages = List.from(MockData.initialMessages);
  }

  @override
  void dispose() {
    _controller.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _sendMessage() {
    final text = _controller.text.trim();
    if (text.isEmpty) return;

    setState(() {
      _messages.add(ChatMessage(
        text: text,
        isUser: true,
        time: _getCurrentTime(),
      ));
    });

    _controller.clear();
    _scrollToBottom();

    // 模拟AI回复
    Future.delayed(const Duration(milliseconds: 1200), () {
      if (mounted) {
        setState(() {
          _messages.add(ChatMessage(
            text: MockData.aiResponses[_aiResponseIndex % MockData.aiResponses.length],
            isUser: false,
            time: _getCurrentTime(),
          ));
          _aiResponseIndex++;
        });
        _scrollToBottom();
      }
    });
  }

  void _scrollToBottom() {
    Future.delayed(const Duration(milliseconds: 100), () {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOutCubic,
        );
      }
    });
  }

  String _getCurrentTime() {
    final now = DateTime.now();
    return '${now.hour}:${now.minute.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // 头部
        _buildHeader(context),

        // 消息区域
        Expanded(
          child: _buildMessageList(),
        ),

        // 输入区域
        _buildInputArea(context),
      ],
    );
  }

  Widget _buildHeader(BuildContext context) {
    return Container(
      padding: const EdgeInsets.only(
        top: 8,
        left: 24,
        right: 24,
        bottom: 16,
      ),
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(
            color: Colors.black.withValues(alpha: 0.04),
            width: 1,
          ),
        ),
      ),
      child: Row(
        children: [
          // AI头像
          Stack(
            children: [
              const CircleAvatar(
                radius: 26,
                backgroundColor: AppTheme.accentSage,
                child: Text('🔮', style: TextStyle(fontSize: 24)),
              ),
              Positioned(
                bottom: 2,
                right: 2,
                child: Container(
                  width: 12,
                  height: 12,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: const Color(0xFF4ADE80),
                    border: Border.all(color: AppTheme.bgCard, width: 2),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(width: 16),

          // 信息
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('星语 Oracle', style: AppTheme.bodyLarge),
              Text('随时为你解读星辰密语', style: AppTheme.bodySmall),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildMessageList() {
    return Container(
      color: AppTheme.bgBase,
      child: ListView.builder(
        controller: _scrollController,
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
        itemCount: _messages.length,
        itemBuilder: (context, index) {
          return ChatBubble(message: _messages[index]);
        },
      ),
    );
  }

  Widget _buildInputArea(BuildContext context) {
    return Container(
      padding: EdgeInsets.only(
        left: 24,
        right: 24,
        top: 16,
        bottom: MediaQuery.of(context).padding.bottom + 20,
      ),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: AppTheme.bgCard,
          borderRadius: BorderRadius.circular(28),
          border: Border.all(
            color: Colors.black.withValues(alpha: 0.06),
            width: 1,
          ),
        ),
        child: Row(
          children: [
            Expanded(
              child: TextField(
                controller: _controller,
                style: AppTheme.bodyMedium,
                decoration: InputDecoration(
                  hintText: '问问星星...',
                  hintStyle: TextStyle(color: AppTheme.textLight),
                  border: InputBorder.none,
                  contentPadding: EdgeInsets.zero,
                ),
                onSubmitted: (_) => _sendMessage(),
              ),
            ),
            const SizedBox(width: 12),
            GestureDetector(
              onTap: _sendMessage,
              child: const CircleAvatar(
                radius: 20,
                backgroundColor: AppTheme.accentSage,
                child: Icon(
                  Icons.send_rounded,
                  color: AppTheme.white,
                  size: 18,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
