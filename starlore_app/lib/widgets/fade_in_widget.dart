import 'package:flutter/material.dart';
import '../theme/tokens.dart';

/// fadeInUp 入场动画包装器
class FadeInUp extends StatefulWidget {
  final Widget child;
  final Duration delay;
  final Duration duration;
  final double offsetY;

  const FadeInUp({
    super.key,
    required this.child,
    this.delay = Duration.zero,
    this.duration = Tok.entrance,
    this.offsetY = 16,
  });

  @override
  State<FadeInUp> createState() => _FadeInUpState();
}

class _FadeInUpState extends State<FadeInUp>
    with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  late Animation<double> _opacity;
  late Animation<Offset> _offset;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(vsync: this, duration: widget.duration);
    _opacity = CurvedAnimation(parent: _ctrl, curve: Tok.easeOutQuart);
    _offset = Tween<Offset>(
      begin: Offset(0, widget.offsetY),
      end: Offset.zero,
    ).animate(CurvedAnimation(parent: _ctrl, curve: Tok.easeOutQuart));

    Future.delayed(widget.delay, () {
      if (mounted) _ctrl.forward();
    });
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _ctrl,
      builder: (_, child) {
        return Opacity(
          opacity: _opacity.value,
          child: Transform.translate(
            offset: _offset.value,
            child: child,
          ),
        );
      },
      child: widget.child,
    );
  }
}

/// 交错入场的列表项包装器
class StaggeredFadeIn extends StatelessWidget {
  final int index;
  final Widget child;
  final Duration staggerDelay;

  const StaggeredFadeIn({
    super.key,
    required this.index,
    required this.child,
    this.staggerDelay = const Duration(milliseconds: 50),
  });

  @override
  Widget build(BuildContext context) {
    return FadeInUp(
      delay: Duration(milliseconds: staggerDelay.inMilliseconds * index),
      child: child,
    );
  }
}
