import 'package:flutter/material.dart';

import '../data/ferrari_models.dart';
import '../main.dart';
import 'models_list_screen.dart';
import 'timeline_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: FerrariHistoryApp.nero,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: SingleChildScrollView(
            child: ConstrainedBox(
              constraints: BoxConstraints(
                minHeight: MediaQuery.of(context).size.height - 48,
              ),
              child: IntrinsicHeight(
                child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 24),
              ClipRRect(
                borderRadius: BorderRadius.circular(22),
                child: Image.asset(
                  'assets/ferrari-icon.png',
                  width: 96,
                  height: 96,
                  fit: BoxFit.cover,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                'FERRARI',
                style: Theme.of(context).textTheme.displayLarge?.copyWith(
                      color: FerrariHistoryApp.rossoCorsa,
                      fontWeight: FontWeight.w900,
                      letterSpacing: 6,
                    ),
              ),
              const SizedBox(height: 8),
              Text(
                'Une grande place dans l\'histoire de l\'automobile.',
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      color: Colors.white70,
                    ),
              ),
              const SizedBox(height: 8),
              Text(
                'Depuis 1947, Maranello construit les voitures de sport les plus convoitées au monde.',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Colors.white54,
                    ),
              ),
              const Spacer(),
              _HomeButton(
                icon: Icons.directions_car_filled,
                title: 'Tous les modèles',
                subtitle:
                    '${ferrariModels.length} modèles produits, caractéristiques détaillées et photo de chaque voiture',
                onTap: () => Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => const ModelsListScreen(),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              _HomeButton(
                icon: Icons.timeline,
                title: 'La ligne du temps',
                subtitle:
                    'De la naissance d\'Enzo Ferrari (1898) au Purosangue (2022)',
                onTap: () => Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => const TimelineScreen(),
                  ),
                ),
              ),
              const Spacer(),
              Center(
                child: Text(
                  'Photos : Wikimedia Commons (licences libres)\nApplication non officielle — fan project',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: Colors.white38,
                      ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _HomeButton extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  const _HomeButton({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: const Color(0xFF1E1E1E),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: FerrariHistoryApp.rossoCorsa, width: 1.5),
          ),
          child: Row(
            children: [
              Icon(icon, color: FerrariHistoryApp.rossoCorsa, size: 40),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: Theme.of(context).textTheme.titleLarge?.copyWith(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                          ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      subtitle,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: Colors.white60,
                          ),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: Colors.white54),
            ],
          ),
        ),
      ),
    );
  }
}
