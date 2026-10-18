
import 'package:flutter/material.dart';
import 'login_screen.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;

  late Animation<Offset> _slideAnimation;
  late Animation<double> _rotationAnimation;
  late Animation<double> _scaleAnimation;
  late Animation<double> _nameAnimation;
  late Animation<Offset> _nameSlideAnimation;
  late Animation<double> _taglineAnimation;

  @override
  void initState() {
    super.initState();

    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 3000),
    );

    // Logo enters from the upper-right.
    _slideAnimation = Tween<Offset>(
      begin: const Offset(2.0, -2.0),
      end: Offset.zero,
    ).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.0, 0.50, curve: Curves.easeOutCubic),
      ),
    );

    // Logo rotates while entering.
    _rotationAnimation = Tween<double>(
      begin: 1.0,
      end: 0.0,
    ).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.0, 0.50, curve: Curves.easeOutCubic),
      ),
    );

    // Logo grows into its final size.
    _scaleAnimation = Tween<double>(
      begin: 0.4,
      end: 1.0,
    ).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.0, 0.55, curve: Curves.easeOutBack),
      ),
    );

    // Nexora text appears.
    _nameAnimation = CurvedAnimation(
      parent: _controller,
      curve: const Interval(0.50, 0.75, curve: Curves.easeIn),
    );

    _nameSlideAnimation = Tween<Offset>(
      begin: const Offset(0, 0.5),
      end: Offset.zero,
    ).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.50, 0.75, curve: Curves.easeOut),
      ),
    );

    // Tagline appears last.
    _taglineAnimation = CurvedAnimation(
      parent: _controller,
      curve: const Interval(0.70, 0.90, curve: Curves.easeIn),
    );

    _startAnimation();
  }

  Future<void> _startAnimation() async {
    await _controller.forward();

    // Keep the completed splash screen visible.
    await Future.delayed(const Duration(milliseconds: 1000));

    if (!mounted) return;

    Navigator.pushReplacement(
      context,
      MaterialPageRoute(
        builder: (context) => const LoginScreen(),
      ),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        width: double.infinity,
        height: double.infinity,
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              Color(0xFFFAF7FF),
              Color(0xFFF0F2FF),
              Color(0xFFFFFAFC),
            ],
          ),
        ),
        child: Stack(
          children: [
            // Decorative background circles.
            Positioned(
              top: -50,
              left: -50,
              child: _decorationCircle(200, const Color(0xFFD9CCFF)),
            ),
            Positioned(
              bottom: -70,
              right: -60,
              child: _decorationCircle(220, const Color(0xFFE2C5FF)),
            ),

            // Animated logo, name and tagline.
            Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  SlideTransition(
                    position: _slideAnimation,
                    child: RotationTransition(
                      turns: _rotationAnimation,
                      child: ScaleTransition(
                        scale: _scaleAnimation,
                        child: Container(
                          width: 190,
                          height: 190,
                          alignment: Alignment.center,
                          child: Image.asset(
                            'assets/images/nexora_logo.png',
                            fit: BoxFit.contain,
                            errorBuilder: (context, error, stackTrace) {
                              return const Icon(
                                Icons.hub_rounded,
                                size: 150,
                                color: Color(0xFF4320B8),
                              );
                            },
                          ),
                        ),
                      ),
                    ),
                  ),

                  const SizedBox(height: 25),

                  FadeTransition(
                    opacity: _nameAnimation,
                    child: SlideTransition(
                      position: _nameSlideAnimation,
                      child: const Text(
                        'Nexora',
                        style: TextStyle(
                          fontSize: 54,
                          fontWeight: FontWeight.w800,
                          color: Color(0xFF4020B8),
                          letterSpacing: -1.5,
                        ),
                      ),
                    ),
                  ),

                  const SizedBox(height: 12),

                  FadeTransition(
                    opacity: _taglineAnimation,
                    child: const Text(
                      'Connect. Collaborate. Belong.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 17,
                        letterSpacing: 1.1,
                        color: Color(0xFF65617F),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _decorationCircle(double size, Color color) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: RadialGradient(
          colors: [
            color.withValues(alpha: 0.65),
            color.withValues(alpha: 0.0),
          ],
        ),
      ),
    );
  }
}