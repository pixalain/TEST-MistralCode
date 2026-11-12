class CarModel {
  final String id;
  final String name;
  final int yearStart;
  final int? yearEnd;
  final String category;
  final String description;
  final String engine;
  final String power;
  final String torque;
  final String acceleration0to100;
  final String topSpeed;
  final String weight;
  final String production;
  final String imageUrl;
  final String imageCredit;
  final List<String> celebrities;

  const CarModel({
    required this.id,
    required this.name,
    required this.yearStart,
    this.yearEnd,
    required this.category,
    required this.description,
    required this.engine,
    required this.power,
    required this.torque,
    required this.acceleration0to100,
    required this.topSpeed,
    required this.weight,
    required this.production,
    required this.imageUrl,
    required this.imageCredit,
    this.celebrities = const [],
  });

  String get years => yearEnd == null ? '$yearStart' : '$yearStart–$yearEnd';
}
