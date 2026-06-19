import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

/// 动态光球渐变背景 — 完美的 Web 端 Canvas 气泡背景移植
class OrbBackground extends StatefulWidget {
  const OrbBackground({super.key});

  @override
  State<OrbBackground> createState() => _OrbBackgroundState();
}

class _OrbBackgroundState extends State<OrbBackground>
    with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  final List<_Bubble> _bubbles = [];
  double _lastWidth = 0;
  double _lastHeight = 0;

  @override
  void initState() {
    super.initState();
    // 持续不断的低频 Ticker 驱动物理引擎更新
    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 20),
    )..addListener(() {
        _updatePhysics();
      })..repeat();
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  void _initBubbles(double width, double height, List<Color> colors) {
    _bubbles.clear();
    final rand = math.Random();

    const count = 6;
    const minRadius = 150.0;
    const maxRadius = 300.0;

    for (int i = 0; i < count; i++) {
      final r = minRadius + rand.nextDouble() * (maxRadius - minRadius);
      final x = rand.nextDouble() * width;
      // 限制气泡出现在屏幕中下部 (对齐 Web 端的 BOTTOM_BAND_START = 0.6)
      final y = (0.5 + rand.nextDouble() * 0.7) * height;

      _bubbles.add(_Bubble(
        x: x,
        y: y,
        r: r,
        color: colors[i % colors.length],
        vx: (rand.nextDouble() - 0.5) * 0.3,
        vy: (rand.nextDouble() - 0.5) * 0.3,
        jitter: 0.6 + rand.nextDouble() * 0.8,
        blur: 120.0 + rand.nextDouble() * 120.0,
      ));
    }
    _lastWidth = width;
    _lastHeight = height;
  }

  void _updateColors(List<Color> colors) {
    for (int i = 0; i < _bubbles.length; i++) {
      _bubbles[i].color = colors[i % colors.length];
    }
  }

  List<Color> _getBubbleColors(String key) {
    switch (key) {
      case 'white':
        return const [Color(0xFFD8D8D8), Color(0xFFE8E8E8), Color(0xFFF0F0F0)];
      case 'dark':
        return const [Color(0xFF1E3A5F), Color(0xFF2D1B4E), Color(0xFF0D3B3B)];
      case 'green':
        return const [Color(0xFFA8E6CF), Color(0xFFDCEDC1), Color(0xFFFFD3B6)];
      case 'blue':
        return const [Color(0xFFF7DA39), Color(0xFF8FDBE9), Color(0xFFFFFFFF)];
      case 'pink':
        return const [Color(0xFFFF9A9E), Color(0xFFFECFEF), Color(0xFFFFD93D)];
      case 'default':
      default:
        return const [
          Color(0xFFFFB7B2), // Macaron Pink (樱花粉)
          Color(0xFFFFDAC1), // Macaron Peach (蜜桃橘)
          Color(0xFFE2F0CB), // Macaron Yellow-Green (柠檬黄绿)
          Color(0xFFB5EAD7), // Macaron Mint (薄荷绿)
          Color(0xFFC7CEEA), // Macaron Lavender (薰衣草紫)
        ];
    }
  }

  void _updatePhysics() {
    if (_bubbles.isEmpty || _lastWidth == 0 || _lastHeight == 0) return;

    final t = DateTime.now().millisecondsSinceEpoch * 0.00015; // 缓速时间轴
    const speed = 0.06;
    final bottomBandStart = _lastHeight * 0.45;
    final bottomBandEnd = _lastHeight * 1.3;

    for (int i = 0; i < _bubbles.length; i++) {
      final b = _bubbles[i];

      // 使用复合正弦/余弦波拟合二维噪声漂移，提供极致平滑的漂浮轨迹
      final angle = (math.sin(b.x * 0.002 + t * 0.1) + math.cos(b.y * 0.002 + t * 0.15)) * math.pi * 2;
      final fx = math.cos(angle) * speed * b.jitter;
      final fy = math.sin(angle) * speed * b.jitter;

      // 气泡间的微弱斥力，防止多球交叠重合
      double sx = 0;
      double sy = 0;
      for (int j = 0; j < _bubbles.length; j++) {
        if (j != i) {
          final o = _bubbles[j];
          final dx = b.x - o.x;
          final dy = b.y - o.y;
          final d2 = dx * dx + dy * dy;
          final minD = (b.r + o.r) * 0.55;
          if (d2 < minD * minD && d2 > 0.01) {
            final d = math.sqrt(d2);
            final push = (minD - d) / minD;
            sx += (dx / d) * push * 0.12;
            sy += (dy / d) * push * 0.12;
          }
        }
      }

      // 重力感约束机制，拉回目标中下部频带
      double by = 0;
      if (b.y < bottomBandStart) by += (bottomBandStart - b.y) * 0.003;
      if (b.y > bottomBandEnd) by -= (b.y - bottomBandEnd) * 0.003;

      b.vx = (b.vx + fx + sx) * 0.95;
      b.vy = (b.vy + fy + sy + by) * 0.95;

      // 限制最高移动速度，避免跳变
      final vel = math.sqrt(b.vx * b.vx + b.vy * b.vy);
      const maxVel = 0.5;
      if (vel > maxVel) {
        b.vx = (b.vx / vel) * maxVel;
        b.vy = (b.vy / vel) * maxVel;
      }

      b.x += b.vx;
      b.y += b.vy;

      // 水平 wrap-around 循环
      final margin = b.r + b.blur * 0.5;
      if (b.x < -margin) b.x = _lastWidth + margin;
      if (b.x > _lastWidth + margin) b.x = -margin;

      // 限制垂直范围
      b.y = b.y.clamp(bottomBandStart - b.r * 0.5, bottomBandEnd + b.r * 0.5);
    }
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);

    return Positioned.fill(
      child: Container(
        color: p.canvas,
        child: LayoutBuilder(
          builder: (context, constraints) {
            final width = constraints.maxWidth;
            final height = constraints.maxHeight;
            if (width == 0 || height == 0) return const SizedBox.shrink();

            final colors = _getBubbleColors(p.key);
            if (_bubbles.isEmpty || _lastWidth != width || _lastHeight != height) {
              _initBubbles(width, height, colors);
            } else {
              _updateColors(colors);
            }

            return AnimatedBuilder(
              animation: _ctrl,
              builder: (context, child) {
                return CustomPaint(
                  painter: _BubblesPainter(
                    bubbles: _bubbles,
                    brightness: p.brightness,
                  ),
                  size: Size(width, height),
                );
              },
            );
          },
        ),
      ),
    );
  }
}

class _Bubble {
  double x;
  double y;
  double r;
  Color color;
  double vx;
  double vy;
  final double jitter;
  final double blur;

  _Bubble({
    required this.x,
    required this.y,
    required this.r,
    required this.color,
    required this.vx,
    required this.vy,
    required this.jitter,
    required this.blur,
  });
}

class _BubblesPainter extends CustomPainter {
  final List<_Bubble> bubbles;
  final Brightness brightness;

  _BubblesPainter({required this.bubbles, required this.brightness});

  @override
  void paint(Canvas canvas, Size size) {
    final isDark = brightness == Brightness.dark;
    // 对齐 Web 端对深色/浅色下的透明度优化配置
    final opacity = isDark ? 0.45 : 0.65;

    for (final b in bubbles) {
      final paint = Paint()
        ..color = b.color.withValues(alpha: opacity)
        ..style = PaintingStyle.fill
        ..maskFilter = MaskFilter.blur(BlurStyle.normal, b.blur);

      canvas.drawCircle(Offset(b.x, b.y), b.r, paint);
    }
  }

  @override
  bool shouldRepaint(covariant _BubblesPainter oldDelegate) => true;
}
