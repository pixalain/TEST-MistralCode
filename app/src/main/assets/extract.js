(function () {
  function txt(e) { return (e.textContent || '').replace(/\s+/g, ' ').trim(); }
  var DATE_RE = /(Lundi|Mardi|Mercredi|Jeudi|Vendredi|Samedi|Dimanche)\s+\d{1,2}\s+(janvier|février|mars|avril|mai|juin|juillet|août|septembre|octobre|novembre|décembre)\s+\d{4}(?:\s*[àa]\s*(\d{1,2}h\d{2}))?/i;
  var STOP = /(À partir de|A partir de|Gratuit|Réserver|Reserver|Annulé|Annule|Plus réservable|Plus reservable|d'infos|d'info|Première partie|Dans le cadre du festival)/i;
  var seen = {};
  var out = [];
  var links = document.querySelectorAll('a[href*="/concert/"]');
  for (var i = 0; i < links.length; i++) {
    var a = links[i];
    var href = a.href;
    if (!href || !/\.html(\?.*)?$/.test(href)) continue;
    if (seen[href]) continue;
    var el = a;
    var block = null;
    while (el && el !== document.body) {
      var t = txt(el);
      if (DATE_RE.test(t) && t.length < 2000) { block = el; break; }
      el = el.parentElement;
    }
    if (!block) continue;
    var t = txt(block);
    var m = t.match(DATE_RE);
    if (!m) continue;
    var datePart = m[0].split(/\s*[àa]\s*\d{1,2}h\d{2}/i)[0].trim();
    var time = m[3] || '';
    var after = t.substring(m.index + m[0].length).trim();
    var sm = after.match(STOP);
    var venue = sm ? after.substring(0, sm.index).trim() : after;
    var city = '';
    var p = block;
    for (var j = 0; j < 6 && p && !city; j++) {
      var s = p.previousElementSibling;
      while (s) {
        var cm = txt(s).match(/(.+?)\s*\((\d+[AB]?)\)/);
        if (cm) { city = cm[1].trim(); break; }
        s = s.previousElementSibling;
      }
      p = p.parentElement;
    }
    var name = '';
    var h = block.querySelector('h1,h2,h3,h4');
    if (h && txt(h)) name = txt(h);
    if (!name) {
      var before = t.substring(0, m.index).trim();
      var pm = before.match(/(.+?)\s*\((\d+[AB]?)\),?\s*$/);
      if (pm) before = pm[1].trim();
      name = before;
    }
    var price = '';
    var prm = t.match(/(À partir de|A partir de)\s*([0-9]+[.,][0-9]{2})\s*€?/i);
    if (prm) price = 'À partir de ' + prm[2] + ' €';
    else if (/Gratuit/i.test(t)) price = 'Gratuit';
    var book = '';
    var bl = block.querySelectorAll('a[href]');
    for (var k = 0; k < bl.length; k++) {
      var l = txt(bl[k]).toLowerCase();
      if (l.indexOf('réserver') >= 0 || l.indexOf('reserver') >= 0 || l.indexOf('billetterie') >= 0) {
        book = bl[k].href;
        break;
      }
    }
    seen[href] = 1;
    out.push({
      name: name,
      city: city,
      date: datePart,
      time: time,
      venue: venue,
      price: price,
      bookingUrl: book,
      infoUrl: href
    });
  }
  return JSON.stringify(out);
})();
