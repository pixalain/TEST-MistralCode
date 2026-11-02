import 'package:flutter/material.dart';

import 'screens/home_screen.dart';

void main() {
  runApp(const FerrariHistoryApp());
}

class FerrariHistoryApp extends StatelessWidget {
  const FerrariHistoryApp({super.key});

  static const Color rossoCorsa = Color(0xFFD40000);
  static const Color nero = Color(0xFF111111);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Ferrari History',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: rossoCorsa,
          brightness: Brightness.light,
        ),
        useMaterial3: true,
        appBarTheme: const AppBarTheme(
          backgroundColor: nero,
          foregroundColor: Colors.white,
        ),
        cardTheme: CardTheme(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
        ),
      ),
      darkTheme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: rossoCorsa,
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
        appBarTheme: const AppBarTheme(
          backgroundColor: nero,
          foregroundColor: Colors.white,
        ),
        cardTheme: CardTheme(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
        ),
      ),
      home: const HomeScreen(),
    );
  }
}
