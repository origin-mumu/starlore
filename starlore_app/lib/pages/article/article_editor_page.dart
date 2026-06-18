import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../theme/tokens.dart';
import '../../theme/typography.dart';
import '../../services/article_service.dart';
import '../../widgets/glass_card.dart';
import '../../widgets/fade_in_widget.dart';

/// 文章编辑/发布页
class ArticleEditorPage extends StatefulWidget {
  final int? articleId; // null = 新建, 非null = 编辑
  final String? initialTitle;
  final String? initialContent;
  final String? initialDescription;
  final String? initialCategory;
  final String? initialCoverImage;
  final bool initialIsPublic;

  const ArticleEditorPage({
    super.key,
    this.articleId,
    this.initialTitle,
    this.initialContent,
    this.initialDescription,
    this.initialCategory,
    this.initialCoverImage,
    this.initialIsPublic = true,
  });

  @override
  State<ArticleEditorPage> createState() => _ArticleEditorPageState();
}

class _ArticleEditorPageState extends State<ArticleEditorPage> {
  final _titleCtrl = TextEditingController();
  final _contentCtrl = TextEditingController();
  final _descCtrl = TextEditingController();
  final _coverCtrl = TextEditingController();
  String _category = '星座';
  bool _isPublic = true;
  bool _saving = false;
  bool _loadingCategories = true;
  List<String> _categories = [];

  bool get _isEditing => widget.articleId != null;

  @override
  void initState() {
    super.initState();
    if (widget.initialTitle != null) _titleCtrl.text = widget.initialTitle!;
    if (widget.initialContent != null) _contentCtrl.text = widget.initialContent!;
    if (widget.initialDescription != null) _descCtrl.text = widget.initialDescription!;
    if (widget.initialCategory != null) _category = widget.initialCategory!;
    if (widget.initialCoverImage != null) _coverCtrl.text = widget.initialCoverImage!;
    _isPublic = widget.initialIsPublic;
    _loadCategories();
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _contentCtrl.dispose();
    _descCtrl.dispose();
    _coverCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadCategories() async {
    final cats = await ArticleService.getPublicCategories();
    if (mounted) {
      setState(() {
        _categories = cats.map((c) => c.name).toList();
        if (!_categories.contains(_category) && _categories.isNotEmpty) {
          _category = _categories.first;
        }
        _loadingCategories = false;
      });
    }
  }

  Future<void> _save() async {
    final title = _titleCtrl.text.trim();
    final content = _contentCtrl.text.trim();

    if (title.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请输入文章标题')),
      );
      return;
    }
    if (content.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请输入文章内容')),
      );
      return;
    }

    setState(() => _saving = true);

    final description = _descCtrl.text.trim().isEmpty
        ? content.substring(0, content.length.clamp(0, 100))
        : _descCtrl.text.trim();
    final coverImage = _coverCtrl.text.trim().isEmpty
        ? null
        : _coverCtrl.text.trim();

    bool ok;
    String? error;
    if (_isEditing) {
      final result = await ArticleService.updateArticle(
        id: widget.articleId!,
        title: title,
        content: content,
        description: description,
        coverImage: coverImage,
        category: _category,
        isPublic: _isPublic,
      );
      ok = result.ok;
      error = result.error;
    } else {
      final result = await ArticleService.createArticle(
        title: title,
        content: content,
        description: description,
        coverImage: coverImage,
        category: _category,
        isPublic: _isPublic,
      );
      ok = result.ok;
      error = result.error;
    }

    if (mounted) {
      setState(() => _saving = false);
      if (ok) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(_isEditing ? '文章已更新' : '文章已发布')),
        );
        Navigator.of(context).pop(true);
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(error ?? '操作失败')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);

    return Scaffold(
      backgroundColor: p.canvas,
      body: Stack(
        children: [
          SafeArea(
            child: Column(
              children: [
                _buildHeader(p),
                Expanded(
                  child: SingleChildScrollView(
                    physics: const BouncingScrollPhysics(),
                    padding: const EdgeInsets.symmetric(
                        horizontal: Tok.horizontalPadding),
                    child: FadeInUp(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const SizedBox(height: Tok.space4),
                          // 标题输入
                          _buildTextField(
                            p,
                            controller: _titleCtrl,
                            hint: '文章标题',
                            style: Typo.display(p.ink),
                            maxLines: 1,
                          ),
                          const SizedBox(height: Tok.space4),
                          // 简介
                          _buildTextField(
                            p,
                            controller: _descCtrl,
                            hint: '文章简介（可选，留空自动截取正文前100字）',
                            maxLines: 2,
                          ),
                          const SizedBox(height: Tok.space4),
                          // 封面图 URL
                          _buildTextField(
                            p,
                            controller: _coverCtrl,
                            hint: '封面图 URL（可选）',
                            maxLines: 1,
                            prefixIcon: Icons.image_outlined,
                          ),
                          const SizedBox(height: Tok.space4),
                          // 分类选择
                          _buildCategorySelector(p),
                          const SizedBox(height: Tok.space4),
                          // 正文
                          _buildTextField(
                            p,
                            controller: _contentCtrl,
                            hint: '正文内容（支持 Markdown）',
                            style: Typo.body(p.ink),
                            maxLines: null,
                          ),
                          const SizedBox(height: Tok.space5),
                          // 发布设置
                          _buildPublishSettings(p),
                          const SizedBox(height: Tok.space8),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHeader(StarlorePalette p) {
    return Padding(
      padding: const EdgeInsets.symmetric(
          horizontal: Tok.space3, vertical: Tok.space2),
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.of(context).pop(),
            icon: Icon(Icons.arrow_back_ios_rounded, size: 18, color: p.ink),
          ),
          const Spacer(),
          Text(
            _isEditing ? '编辑文章' : '发布文章',
            style: Typo.h2(p.ink),
          ),
          const Spacer(),
          // 发布/保存按钮
          GestureDetector(
            onTap: _saving ? null : _save,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              decoration: BoxDecoration(
                color: _saving ? p.accent.withValues(alpha: 0.5) : p.accent,
                borderRadius: BorderRadius.circular(Tok.radiusFull),
              ),
              child: _saving
                  ? SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : Text(
                      _isEditing ? '保存' : '发布',
                      style: Typo.label(Colors.white),
                    ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTextField(
    StarlorePalette p, {
    required TextEditingController controller,
    required String hint,
    TextStyle? style,
    int? maxLines,
    IconData? prefixIcon,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: p.surface,
        borderRadius: BorderRadius.circular(Tok.radiusMd),
        border: Border.all(color: p.border.withValues(alpha: 0.5), width: 0.5),
      ),
      child: TextField(
        controller: controller,
        style: style ?? Typo.body(p.ink),
        maxLines: maxLines,
        decoration: InputDecoration(
          hintText: hint,
          hintStyle: Typo.body(p.inkMuted),
          prefixIcon: prefixIcon != null
              ? Icon(prefixIcon, size: 18, color: p.inkMuted)
              : null,
          border: InputBorder.none,
          contentPadding: const EdgeInsets.all(Tok.space4),
        ),
      ),
    );
  }

  Widget _buildCategorySelector(StarlorePalette p) {
    return GlassCard(
      blur: false,
      padding: const EdgeInsets.all(Tok.space4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('分类', style: Typo.caption(p.inkMuted)),
          const SizedBox(height: Tok.space2),
          if (_loadingCategories)
            const Center(
              child: SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(strokeWidth: 1.5),
              ),
            )
          else
            Wrap(
              spacing: Tok.space2,
              runSpacing: Tok.space2,
              children: _categories.map((cat) {
                final selected = _category == cat;
                return GestureDetector(
                  onTap: () => setState(() => _category = cat),
                  child: AnimatedContainer(
                    duration: Tok.fast,
                    padding: const EdgeInsets.symmetric(
                        horizontal: 12, vertical: 6),
                    decoration: BoxDecoration(
                      color: selected ? p.accentSoft : p.canvasDeep,
                      borderRadius: BorderRadius.circular(Tok.radiusFull),
                      border: Border.all(
                        color: selected
                            ? p.accent.withValues(alpha: 0.3)
                            : p.border.withValues(alpha: 0.5),
                        width: 0.5,
                      ),
                    ),
                    child: Text(
                      cat,
                      style: Typo.caption(selected ? p.accent : p.inkSoft),
                    ),
                  ),
                );
              }).toList(),
            ),
        ],
      ),
    );
  }

  Widget _buildPublishSettings(StarlorePalette p) {
    return GlassCard(
      blur: false,
      padding: const EdgeInsets.symmetric(
          horizontal: Tok.space4, vertical: Tok.space3),
      child: Row(
        children: [
          Icon(
            _isPublic ? Icons.public_rounded : Icons.lock_outline_rounded,
            size: 18,
            color: _isPublic ? p.accent : p.inkMuted,
          ),
          const SizedBox(width: Tok.space3),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _isPublic ? '公开发布' : '仅自己可见',
                  style: Typo.body(p.ink),
                ),
                Text(
                  _isPublic ? '所有人可以阅读此文章' : '只有你可以查看此文章',
                  style: Typo.caption(p.inkMuted),
                ),
              ],
            ),
          ),
          Switch(
            value: _isPublic,
            onChanged: (v) => setState(() => _isPublic = v),
            activeThumbColor: p.accent,
          ),
        ],
      ),
    );
  }
}
