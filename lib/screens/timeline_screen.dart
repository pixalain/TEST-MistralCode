import 'package:flutter/material.dart';

import '../data/ferrari_models.dart';
import '../main.dart';

class TimelineScreen extends StatelessWidget {
  const TimelineScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Ligne du temps Ferrari')),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: timelineEvents.length,
        itemBuilder: (context, index) {
          final event = timelineEvents[index];
          final year = event.split(' —').first.trim();
          return IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Column(
                  children: [
                    Container(
                      width: 14,
                      height: 14,
                      decoration: const BoxDecoration(
                        color: FerrariHistoryApp.rossoCorsa,
                        shape: BoxShape.circle,
                      ),
                    ),
                    if (index != timelineEvents.length - 1)
                      Expanded(
                        child: Container(
                          width: 2,
                          color: FerrariHistoryApp.rossoCorsa.withOpacity(0.4),
                        ),
                      ),
                  ],
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.only(bottom: 20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          year,
                          style: const TextStyle(
                            fontWeight: FontWeight.w900,
                            color: FerrariHistoryApp.rossoCorsa,
                            fontSize: 16,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(event.substring(year.length + 3)),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
