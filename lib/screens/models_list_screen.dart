import 'package:flutter/material.dart';

import '../data/ferrari_models.dart';
import '../main.dart';
import '../models/car_model.dart';
import 'car_detail_screen.dart';

class ModelsListScreen extends StatefulWidget {
  const ModelsListScreen({super.key});

  @override
  State<ModelsListScreen> createState() => _ModelsListScreenState();
}

class _ModelsListScreenState extends State<ModelsListScreen> {
  String _query = '';

  List<CarModel> get _filtered {
    final q = _query.trim().toLowerCase();
    if (q.isEmpty) return ferrariModels;
    return ferrariModels
        .where((m) =>
            m.name.toLowerCase().contains(q) ||
            m.category.toLowerCase().contains(q) ||
            m.years.contains(q))
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    final models = _filtered;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Tous les modèles Ferrari'),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: TextField(
              decoration: InputDecoration(
                hintText: 'Rechercher un modèle… (ex. F40, 250, V12)',
                prefixIcon: const Icon(Icons.search),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              onChanged: (v) => setState(() => _query = v),
            ),
          ),
          Expanded(
            child: ListView.separated(
              itemCount: models.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (context, index) {
                final car = models[index];
                return ListTile(
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 8,
                  ),
                  leading: ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: Image.network(
                      car.imageUrl,
                      width: 96,
                      height: 64,
                      fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => Container(
                        width: 96,
                        height: 64,
                        color: Colors.grey.shade300,
                        child: const Icon(Icons.directions_car),
                      ),
                    ),
                  ),
                  title: Text(
                    car.name,
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                  subtitle: Text('${car.years} · ${car.category} · ${car.power}'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) => CarDetailScreen(car: car),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
