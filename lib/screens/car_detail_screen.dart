import 'package:flutter/material.dart';

import '../main.dart';
import '../models/car_model.dart';

class CarDetailScreen extends StatelessWidget {
  final CarModel car;

  const CarDetailScreen({super.key, required this.car});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(car.name)),
      body: ListView(
        children: [
          AspectRatio(
            aspectRatio: 16 / 9,
            child: Image.network(
              car.imageUrl,
              fit: BoxFit.cover,
              errorBuilder: (_, __, ___) => Container(
                color: Colors.grey.shade300,
                child: const Icon(Icons.directions_car, size: 80),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Chip(
                      label: Text(car.years),
                      backgroundColor: FerrariHistoryApp.rossoCorsa,
                      labelStyle: const TextStyle(color: Colors.white),
                    ),
                    const SizedBox(width: 8),
                    Chip(label: Text(car.category)),
                  ],
                ),
                const SizedBox(height: 16),
                Text(
                  car.description,
                  style: Theme.of(context).textTheme.bodyLarge,
                ),
                const SizedBox(height: 24),
                Text(
                  'Caractéristiques',
                  style: Theme.of(context).textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: FerrariHistoryApp.rossoCorsa,
                      ),
                ),
                const SizedBox(height: 8),
                _SpecTile(icon: Icons.settings, label: 'Moteur', value: car.engine),
                _SpecTile(icon: Icons.flash_on, label: 'Puissance', value: car.power),
                _SpecTile(icon: Icons.speed, label: 'Couple', value: car.torque),
                _SpecTile(
                    icon: Icons.timer, label: '0 à 100 km/h', value: car.acceleration0to100),
                _SpecTile(
                    icon: Icons.rocket_launch,
                    label: 'Vitesse max',
                    value: car.topSpeed),
                _SpecTile(icon: Icons.fitness_center, label: 'Poids', value: car.weight),
                _SpecTile(
                    icon: Icons.factory, label: 'Production', value: car.production),
                if (car.celebrities.isNotEmpty) ...[
                  const SizedBox(height: 24),
                  Text(
                    'Propri\u00e9taires c\u00e9l\u00e8bres',
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.bold,
                          color: FerrariHistoryApp.rossoCorsa,
                        ),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      for (final name in car.celebrities)
                        Chip(
                          avatar: const Icon(Icons.person, size: 18),
                          label: Text(name),
                          backgroundColor: const Color(0xFF2A2A2A),
                          labelStyle: const TextStyle(color: Colors.white),
                        ),
                    ],
                  ),
                ],
                const SizedBox(height: 16),
                Text(
                  'Photo : ${car.imageCredit} — source Wikimedia Commons',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: Colors.grey,
                      ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SpecTile extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;

  const _SpecTile({
    required this.icon,
    required this.label,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4),
      child: ListTile(
        leading: Icon(icon, color: FerrariHistoryApp.rossoCorsa),
        title: Text(label),
        trailing: Text(
          value,
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
      ),
    );
  }
}
