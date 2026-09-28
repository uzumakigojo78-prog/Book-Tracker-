// A colour wheel (hue around, saturation outwards) with a brightness slider,
// quick picks and a hex field. Matches the Android app's picker.
import { esc } from './util.js';

const QUICK = ['#E53935', '#D81B60', '#8E24AA', '#5E35B1', '#3949AB', '#1E88E5', '#00ACC1', '#00897B', '#43A047', '#7CB342', '#FDD835', '#FB8C00', '#6D4C41', '#546E7A'];

function hsvToHex(h, s, v) {
  const f = (n) => { const k = (n + h / 60) % 6; return v - v * s * Math.max(0, Math.min(k, 4 - k, 1)); };
  return '#' + [f(5), f(3), f(1)].map((x) => Math.round(x * 255).toString(16).padStart(2, '0')).join('').toUpperCase();
}
function hexToHsv(hex) {
  const n = parseInt(hex.slice(1), 16);
  const r = ((n >> 16) & 255) / 255, g = ((n >> 8) & 255) / 255, b = (n & 255) / 255;
  const max = Math.max(r, g, b), d = max - Math.min(r, g, b);
  let h = 0;
  if (d) h = max === r ? ((g - b) / d) % 6 : max === g ? (b - r) / d + 2 : (r - g) / d + 4;
  return [(h * 60 + 360) % 360, max ? d / max : 0, max];
}

/** Renders the picker into [root]; calls onChange(hex) as the colour changes. */
export function mountColorWheel(root, hex, onChange) {
  let [h, s, v] = hexToHsv(hex);
  v = Math.max(0.15, v);
  root.innerHTML = `<div class="wheel-wrap"><canvas class="wheel" width="560" height="560" aria-label="Colour wheel"></canvas></div>
    <div class="label" style="margin:14px 0 6px">Brightness</div>
    <div class="bright"><i></i></div>
    <div class="quick">${QUICK.map((c) => `<button class="quick-dot" data-c="${c}" style="background:${c}" aria-label="${c}"></button>`).join('')}</div>
    <div class="row hex-row"><span class="hex-dot"></span>
      <label class="field grow"><input data-hex maxlength="7" value="${esc(hex.toUpperCase())}" autocapitalize="characters" spellcheck="false"><span class="lbl">Hex colour</span><span class="lead hash">#</span></label>
      <input type="color" data-native aria-label="System colour picker"></div>`;
  const canvas = root.querySelector('.wheel'), ctx = canvas.getContext('2d');
  const bright = root.querySelector('.bright'), hexIn = root.querySelector('[data-hex]'), dot = root.querySelector('.hex-dot'), native = root.querySelector('[data-native]');

  function draw() {
    const w = canvas.width, c = w / 2, r = c - 4;
    ctx.clearRect(0, 0, w, w);
    const hue = ctx.createConicGradient(0, c, c);
    for (let i = 0; i <= 12; i++) hue.addColorStop(i / 12, hsvToHex((i * 30) % 360, 1, v));
    ctx.fillStyle = hue;
    ctx.beginPath(); ctx.arc(c, c, r, 0, Math.PI * 2); ctx.fill();
    const white = ctx.createRadialGradient(c, c, 0, c, c, r);
    const g = Math.round(v * 255);
    white.addColorStop(0, `rgb(${g},${g},${g})`);
    white.addColorStop(1, `rgba(${g},${g},${g},0)`);
    ctx.fillStyle = white;
    ctx.beginPath(); ctx.arc(c, c, r, 0, Math.PI * 2); ctx.fill();
    const a = (h * Math.PI) / 180, x = c + Math.cos(a) * s * r, y = c + Math.sin(a) * s * r;
    ctx.fillStyle = hsvToHex(h, s, v);
    ctx.lineWidth = 8; ctx.strokeStyle = '#fff';
    ctx.beginPath(); ctx.arc(x, y, 26, 0, Math.PI * 2); ctx.fill(); ctx.stroke();
    ctx.lineWidth = 2; ctx.strokeStyle = 'rgba(0,0,0,.35)';
    ctx.beginPath(); ctx.arc(x, y, 31, 0, Math.PI * 2); ctx.stroke();
    bright.style.background = `linear-gradient(to right, #000, ${hsvToHex(h, s, 1)})`;
    bright.querySelector('i').style.left = `${v * 100}%`;
    const out = hsvToHex(h, s, v);
    dot.style.background = out;
    native.value = out.toLowerCase();
    return out;
  }
  const emit = () => { const out = draw(); if (document.activeElement !== hexIn) hexIn.value = out; onChange(out); };

  const drag = (el, handle) => {
    el.addEventListener('pointerdown', (e) => {
      el.setPointerCapture(e.pointerId);
      handle(e);
      const move = (ev) => handle(ev);
      const up = () => { el.removeEventListener('pointermove', move); el.removeEventListener('pointerup', up); };
      el.addEventListener('pointermove', move);
      el.addEventListener('pointerup', up);
    });
  };
  drag(canvas, (e) => {
    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left - rect.width / 2, y = e.clientY - rect.top - rect.height / 2;
    h = ((Math.atan2(y, x) * 180) / Math.PI + 360) % 360;
    s = Math.min(1, Math.hypot(x, y) / (rect.width / 2 - 2));
    emit();
  });
  drag(bright, (e) => {
    const rect = bright.getBoundingClientRect();
    v = Math.min(1, Math.max(0.15, (e.clientX - rect.left) / rect.width));
    emit();
  });
  const setHex = (value) => {
    [h, s, v] = hexToHsv(value);
    v = Math.max(0.15, v);
    emit();
  };
  root.querySelectorAll('[data-c]').forEach((b) => { b.onclick = () => setHex(b.dataset.c); });
  hexIn.addEventListener('input', () => {
    const m = hexIn.value.replace(/[^0-9a-f]/gi, '').slice(0, 6);
    if (m.length === 6) setHex('#' + m);
  });
  native.addEventListener('input', () => setHex(native.value));
  draw();
}
