import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:ferrari_history/main.dart';
import 'package:ferrari_history/data/ferrari_models.dart';

void main() {
  testWidgets('L\'application démarre sur l\'écran d\'accueil',
      (WidgetTester tester) async {
    await tester.pumpWidget(const FerrariHistoryApp());
    expect(find.text('FERRARI'), findsOneWidget);
    expect(find.text('Tous les modèles'), findsOneWidget);
    expect(find.text('La ligne du temps'), findsOneWidget);
  });

  test('La base de données contient des modèles avec photo et caractéristiques', () {
    expect(ferrariModels.length, greaterThanOrEqualTo(25));
    for (final car in ferrariModels) {
      expect(car.name, isNotEmpty);
      expect(car.imageUrl, startsWith('https://upload.wikimedia.org/'));
      expect(car.power, isNotEmpty);
      expect(car.topSpeed, isNotEmpty);
      expect(car.engine, isNotEmpty);
      expect(car.description, isNotEmpty);
    }
  });

  testWidgets('La liste affiche les modèles et la recherche filtre',
      (WidgetTester tester) async {
    await tester.pumpWidget(const FerrariHistoryApp());
    await tester.tap(find.text('Tous les modèles'));
    await tester.pumpAndSettle();
    expect(find.text('Ferrari 125 S'), findsOneWidget);
    expect(find.text('Ferrari 250 GTO'), findsOneWidget);
    await tester.enterText(find.byType(TextField), 'F40');
    await tester.pumpAndSettle();
    expect(find.text('Ferrari F40'), findsOneWidget);
    expect(find.text('Ferrari 250 GTO'), findsNothing);
  });

  testWidgets('Le détail d\'un modèle affiche ses caractéristiques',
      (WidgetTester tester) async {
    await tester.pumpWidget(const FerrariHistoryApp());
    await tester.tap(find.text('Tous les modèles'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), 'F40');
    await tester.pumpAndSettle();
    await tester.tap(find.text('Ferrari F40'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));
    tester.takeException();
    await tester.pump();
    expect(find.text('Caractéristiques'), findsOneWidget);
    expect(find.text('478 ch'), findsOneWidget);
    expect(find.text('V8 2,9 L biturbo'), findsOneWidget);
  });
}
