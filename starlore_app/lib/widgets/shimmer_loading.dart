import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../theme/tokens.dart';

/// 骨架屏加载占位
class ShimmerLoading extends StatefulWidget {
  final double width;
  final double height;
  final double radius;

  const ShimmerLoading({
    super.key,
    this.width = double.infinity,
    required this.height,
    this.radius = Tok.radiusMd,
  });

  @override
  State<ShimmerLoading> createState() => _ShimmerLoadingState();
}

class _ShimmerLoadingState extends State<ShimmerLoading>
    with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat();
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);

    return AnimatedBuilder(
      animation: _ctrl,
      builder: (context, child) {
        return Container(
          width: widget.width,
          height: widget.height,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(widget.radius),
            gradient: LinearGradient(
              begin: Alignment(-1 + 2 * _ctrl.value, 0),
              end: Alignment(1 + 2 * _ctrl.value, 0),
              colors: [
                p.canvasDeep,
                p.surface,
                p.canvasDeep,
              ],
              stops: const [0.0, 0.5, 1.0],
            ),
          ),
        );
      },
    );
  }
}

/// 文章卡片骨架屏
class ArticleCardSkeleton extends StatelessWidget {
  const ArticleCardSkeleton({super.key});

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);

    return Container(
      margin: const EdgeInsets.only(bottom: Tok.space3),
      padding: const EdgeInsets.all(Tok.space4),
      decoration: BoxDecoration(
        color: p.surface,
        borderRadius: BorderRadius.circular(Tok.radiusLg),
        border: Border.all(color: p.border.withValues(alpha: 0.4), width: 0.5),
      ),
      child: const Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              ShimmerLoading(width: 60, height: 22, radius: 100),
              Spacer(),
              ShimmerLoading(width: 50, height: 14),
            ],
          ),
          SizedBox(height: Tok.space3),
          ShimmerLoading(height: 20),
          SizedBox(height: Tok.space2),
          ShimmerLoading(height: 14),
          SizedBox(height: Tok.space1),
          ShimmerLoading(width: 200, height: 14),
          SizedBox(height: Tok.space4),
          Row(
            children: [
              ShimmerLoading(width: 80, height: 12),
              SizedBox(width: Tok.space4),
              ShimmerLoading(width: 40, height: 12),
            ],
          ),
        ],
      ),
    );
  }
}
