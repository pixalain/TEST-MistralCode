"use strict";

const STORAGE_KEY = "basicfit-tracker-sessions";
const $ = (sel) => document.querySelector(sel);

// ---------- Stockage ----------
let sessions = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");

function saveSessions() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions));
}

// ---------- Outils ----------
function fmtDuration(min) {
  const h = Math.floor(min / 60);
  const m = min % 60;
  return h > 0 ? `${h} h ${m.toString().padStart(2, "0")}` : `${m} min`;
}

function fmtVolume(kg) {
  return kg >= 1000 ? `${(kg / 1000).toFixed(1)} t` : `${Math.round(kg)} kg`;
}

function sessionVolume(s) {
  return s.exercises.reduce((t, e) => t + (e.sets || 0) * (e.reps || 0) * (e.weight || 0), 0);
}

function showToast(msg) {
  const t = $("#toast");
  t.textContent = msg;
  t.classList.add("show");
  setTimeout(() => t.classList.remove("show"), 2200);
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

// ---------- Navigation ----------
document.querySelectorAll(".tab").forEach((tab) => {
  tab.addEventListener("click", () => {
    document.querySelectorAll(".tab").forEach((t) => t.classList.remove("active"));
    document.querySelectorAll(".panel").forEach((p) => p.classList.remove("active"));
    tab.classList.add("active");
    $("#" + tab.dataset.tab).classList.add("active");
  });
});

// ---------- Séance rapide ----------
$("#quick-start").addEventListener("click", () => {
  $("#s-date").value = new Date().toISOString().slice(0, 10);
  document.querySelector('.tab[data-tab="sessions"]').click();
});

// ---------- Exercices du formulaire ----------
const DEFAULT_EXERCISES = [
  "Développé couché", "Développé militaire", "Squat", "Leg press", "Fentes",
  "Tirage vertical", "Tirage horizontal", "Rowing barre", "Biceps curl",
  "Extension triceps", "Élévations latérales", "Deadlift", "Hip thrust",
  "Mollets debout", "Abdos crunch", "Planche", "Tapis de course", "Vélo", "Elliptique", "Rameur",
];

function refreshSuggestions() {
  const names = new Set(DEFAULT_EXERCISES);
  sessions.forEach((s) => s.exercises.forEach((e) => e.name && names.add(e.name)));
  $("#ex-suggestions").innerHTML = [...names].map((n) => `<option value="${escapeHtml(n)}">`).join("");
}

function addExerciseRow() {
  const row = document.createElement("div");
  row.className = "exercise";
  row.innerHTML = `
    <input type="text" class="ex-name" placeholder="Exercice" list="ex-suggestions" />
    <input type="number" class="ex-sets" placeholder="Séries" min="1" />
    <input type="number" class="ex-reps" placeholder="Reps" min="1" />
    <input type="number" class="ex-weight" placeholder="Poids (kg)" min="0" step="0.5" />
    <button type="button" class="btn danger ex-remove">✕</button>`;
  row.querySelector(".ex-remove").addEventListener("click", () => row.remove());
  $("#exercises").appendChild(row);
}

$("#add-exercise").addEventListener("click", addExerciseRow);

// ---------- Formulaire ----------
$("#s-date").value = new Date().toISOString().slice(0, 10);

$("#session-form").addEventListener("submit", (e) => {
  e.preventDefault();
  const exercises = [...document.querySelectorAll(".exercise")]
    .map((row) => ({
      name: row.querySelector(".ex-name").value.trim(),
      sets: parseInt(row.querySelector(".ex-sets").value) || 0,
      reps: parseInt(row.querySelector(".ex-reps").value) || 0,
      weight: parseFloat(row.querySelector(".ex-weight").value) || 0,
    }))
    .filter((ex) => ex.name);

  sessions.push({
    id: Date.now(),
    date: $("#s-date").value,
    type: $("#s-type").value,
    duration: parseInt($("#s-duration").value) || 0,
    club: $("#s-club").value.trim(),
    exercises,
  });
  sessions.sort((a, b) => b.date.localeCompare(a.date));
  saveSessions();
  e.target.reset();
  $("#s-date").value = new Date().toISOString().slice(0, 10);
  $("#s-duration").value = 60;
  $("#exercises").innerHTML = "";
  addExerciseRow();
  renderAll();
  showToast("Séance enregistrée 💪");
});

// ---------- Suppression ----------
function deleteSession(id) {
  if (!confirm("Supprimer cette séance ?")) return;
  sessions = sessions.filter((s) => s.id !== id);
  saveSessions();
  renderAll();
  showToast("Séance supprimée");
}

// ---------- Tableau de bord ----------
function renderStats() {
  $("#stat-sessions").textContent = sessions.length;
  const totalMin = sessions.reduce((t, s) => t + s.duration, 0);
  $("#stat-duration").textContent = fmtDuration(totalMin);
  const totalKg = sessions.reduce((t, s) => t + sessionVolume(s), 0);
  $("#stat-volume").textContent = fmtVolume(totalKg);

  const now = new Date();
  const monday = new Date(now);
  monday.setDate(now.getDate() - ((now.getDay() + 6) % 7));
  monday.setHours(0, 0, 0, 0);
  $("#stat-week").textContent = sessions.filter((s) => new Date(s.date) >= monday)
    .reduce((t, s) => t + s.exercises.reduce((u, e) => u + e.sets, 0), 0);
}

function renderHeatmap() {
  const byDay = {};
  sessions.forEach((s) => {
    byDay[s.date] = (byDay[s.date] || 0) + s.duration;
  });
  const today = new Date();
  const weeks = [];
  for (let w = 11; w >= 0; w--) {
    const monday = new Date(today);
    monday.setDate(today.getDate() - ((today.getDay() + 6) % 7) - w * 7);
    const days = [];
    for (let d = 0; d < 7; d++) {
      const day = new Date(monday);
      day.setDate(monday.getDate() + d);
      const key = day.toISOString().slice(0, 10);
      const dur = byDay[key] || 0;
      const lvl = dur === 0 ? "" : dur < 30 ? "l1" : dur < 70 ? "l2" : "l3";
      days.push(`<div class="day ${lvl}" title="${key} : ${dur} min"></div>`);
    }
    weeks.push(`<div class="week">${days.join("")}</div>`);
  }
  $("#heatmap").innerHTML = weeks.join("");
}

// ---------- Historique ----------
function renderSessions() {
  const list = $("#sessions-list");
  if (!sessions.length) {
    list.innerHTML = `<p class="empty">Aucune séance pour le moment. Allez-y, le club vous attend ! 🏋️</p>`;
    return;
  }
  list.innerHTML = sessions
    .map(
      (s) => `
    <div class="session-item">
      <div>
        <strong>${s.date}</strong> <span class="badge">${escapeHtml(s.type)}</span>
        ${s.club ? `· <em>${escapeHtml(s.club)}</em>` : ""}
        <div class="meta">
          <span>⏱ ${fmtDuration(s.duration)}</span>
          <span>📦 ${s.exercises.length} exercices</span>
          <span>🏋️ ${fmtVolume(sessionVolume(s))}</span>
        </div>
      </div>
      <div class="actions">
        <button class="btn danger" onclick="deleteSession(${s.id})">Supprimer</button>
      </div>
    </div>`
    )
    .join("");
}

// ---------- Graphique (canvas natif) ----------
function renderChart() {
  const canvas = $("#chart");
  const ctx = canvas.getContext("2d");
  const W = (canvas.width = canvas.clientWidth || 600);
  const H = canvas.height;
  ctx.clearRect(0, 0, W, H);

  const data = [...sessions].reverse().slice(-20);
  if (!data.length) {
    ctx.fillStyle = "#9aa0ab";
    ctx.textAlign = "center";
    ctx.fillText("Enregistrez des séances pour voir votre progression", W / 2, H / 2);
    return;
  }
  const vols = data.map(sessionVolume);
  const maxV = Math.max(...vols, 1);
  const pad = 30;
  const bw = (W - pad * 2) / data.length;

  ctx.strokeStyle = "#2a2d38";
  for (let i = 0; i <= 3; i++) {
    const y = pad + ((H - pad * 2) / 3) * i;
    ctx.beginPath();
    ctx.moveTo(pad, y);
    ctx.lineTo(W - pad / 2, y);
    ctx.stroke();
  }
  vols.forEach((v, i) => {
    const h = (v / maxV) * (H - pad * 2);
    const x = pad + i * bw;
    const y = H - pad - h;
    ctx.fillStyle = "#ffcb00";
    ctx.fillRect(x + 2, y, bw - 4, h);
  });
}

// ---------- Export ----------
$("#export-btn").addEventListener("click", () => {
  const text = JSON.stringify(sessions, null, 2);
  if (navigator.share) {
    navigator.share({ title: "mes-seances-basic-fit.json", text }).catch(() => {});
  } else {
    const blob = new Blob([text], { type: "application/json" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "mes-seances-basic-fit.json";
    a.click();
    URL.revokeObjectURL(a.href);
  }
});

// ---------- Init ----------
function renderAll() {
  refreshSuggestions();
  renderStats();
  renderHeatmap();
  renderSessions();
  renderChart();
}

addExerciseRow();
renderAll();

window.deleteSession = deleteSession;
