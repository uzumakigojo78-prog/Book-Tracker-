// Appearance settings (Settings → Theme & colors / Text / Style), matching the Android app.
import { store } from './util.js';

const KEY = 'booktracker.appearance';

export const THEME_MODES = [['system', 'System', 'brightness_auto'], ['light', 'Light', 'light_mode'], ['dark', 'Dark', 'dark_mode']];
export const TEXT_SIZES = [['small', 'Small', 0.9], ['default', 'Default', 1], ['large', 'Large', 1.12], ['xl', 'Extra large', 1.25]];
export const FONTS = [
  ['sans', 'Sans', '"Roboto Flex", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif'],
  ['serif', 'Serif', '"Roboto Serif", Georgia, "Times New Roman", serif'],
  ['mono', 'Mono', '"Roboto Mono", ui-monospace, "SF Mono", Menlo, Consolas, monospace'],
];
export const CORNERS = [['xround', 'Extra round', 1], ['rounded', 'Rounded', 0.6], ['square', 'Square', 0.25]];
export const ICON_STYLES = [['outlined', 'Outlined'], ['rounded', 'Rounded'], ['sharp', 'Sharp'], ['filled', 'Filled']];
export const PALETTES = [['violet', 'Violet'], ['ocean', 'Ocean'], ['forest', 'Forest'], ['sunset', 'Sunset'], ['rose', 'Rose']];

const DEFAULTS = { theme: 'system', palette: 'violet', textSize: 'default', font: 'sans', bold: true, corners: 'xround', icons: 'outlined' };

export let appearance = { ...DEFAULTS, ...(store.get(KEY) || {}) };

export function setAppearance(patch) {
  appearance = patch === null ? { ...DEFAULTS } : { ...appearance, ...patch };
  store.set(KEY, appearance);
  applyAppearance();
}

// [main, onMain, container, onContainer] for light and dark.
const A = {
  violet: [['#6B2BD9', '#FFFFFF', '#E9DDFF', '#22005D'], ['#D0BCFF', '#3B0092', '#5516BE', '#E9DDFF']],
  pink: [['#B0235F', '#FFFFFF', '#FFD9E2', '#3E001D'], ['#FFB1C8', '#650033', '#8E0048', '#FFD9E2']],
  amber: [['#8A5100', '#FFFFFF', '#FFDCBE', '#2C1600'], ['#FFB870', '#4A2800', '#693C00', '#FFDCBE']],
  blue: [['#0061A4', '#FFFFFF', '#D1E4FF', '#001D36'], ['#9ECAFF', '#003258', '#00497D', '#D1E4FF']],
  teal: [['#00696E', '#FFFFFF', '#A0EFF3', '#002022'], ['#4CD9E0', '#003739', '#004F52', '#A0EFF3']],
  green: [['#2E6C00', '#FFFFFF', '#ADF67A', '#0A2100'], ['#92D961', '#173800', '#225100', '#ADF67A']],
  mint: [['#006C4C', '#FFFFFF', '#89F8C7', '#002114'], ['#6CDBAC', '#003826', '#005139', '#89F8C7']],
  orange: [['#A33200', '#FFFFFF', '#FFDBCF', '#3A0B00'], ['#FFB59D', '#5D1800', '#842500', '#FFDBCF']],
  gold: [['#7D5800', '#FFFFFF', '#FFDEA6', '#271900'], ['#F9BC48', '#422C00', '#5F4100', '#FFDEA6']],
};
const PALETTE_ACCENTS = {
  violet: ['violet', 'pink', 'amber'], ocean: ['blue', 'teal', 'violet'], forest: ['green', 'mint', 'amber'],
  sunset: ['orange', 'pink', 'gold'], rose: ['pink', 'violet', 'orange'],
};

/** Primary, secondary and tertiary colours of a palette, for swatches. */
export const paletteSwatch = (p, dark) => PALETTE_ACCENTS[p].map((name) => A[name][dark ? 1 : 0][0]);

const media = matchMedia('(prefers-color-scheme: dark)');
export const isDark = () => (appearance.theme === 'system' ? media.matches : appearance.theme === 'dark');
media.addEventListener('change', () => applyAppearance());

export function applyAppearance() {
  const root = document.documentElement;
  const dark = isDark();
  root.dataset.theme = dark ? 'dark' : 'light';
  root.dataset.bold = appearance.bold ? 'on' : 'off';
  root.dataset.icons = appearance.icons;
  const [p, s, t] = PALETTE_ACCENTS[appearance.palette] || PALETTE_ACCENTS.violet;
  const set = (prefix, name) => {
    const [main, on, container, onContainer] = A[name][dark ? 1 : 0];
    root.style.setProperty(`--${prefix}`, main);
    root.style.setProperty(`--on-${prefix}`, on);
    root.style.setProperty(`--${prefix}-container`, container);
    root.style.setProperty(`--on-${prefix}-container`, onContainer);
  };
  set('primary', p);
  set('secondary', s);
  set('tertiary', t);
  root.style.setProperty('--ts', (TEXT_SIZES.find((x) => x[0] === appearance.textSize) || TEXT_SIZES[1])[2]);
  root.style.setProperty('--rs', (CORNERS.find((x) => x[0] === appearance.corners) || CORNERS[0])[2]);
  root.style.setProperty('--font', (FONTS.find((x) => x[0] === appearance.font) || FONTS[0])[2]);
  document.querySelector('meta[name=theme-color]:not([media])')?.setAttribute('content', dark ? '#151218' : '#FFF8F4');
  loadFonts([appearance.icons === 'filled' ? 'outlined' : appearance.icons], appearance.font);
}

/* ---------- fonts ---------- */

// Every Material Symbol the app shows (Google Fonts subsets to these; must stay sorted).
export const ICON_NAMES = [
  'add', 'arrow_back', 'auto_awesome', 'bar_chart', 'brightness_auto', 'category', 'check', 'check_circle',
  'chevron_right', 'close', 'dark_mode', 'delete', 'download', 'edit', 'error', 'event', 'format_size',
  'history', 'key', 'library_add', 'light_mode', 'link', 'local_fire_department', 'lock', 'menu_book',
  'open_in_new', 'palette', 'person', 'psychology', 'refresh', 'restart_alt', 'save', 'search', 'settings',
  'style', 'text_fields', 'upload', 'visibility', 'visibility_off',
];

const loaded = new Set();
function addStylesheet(id, href) {
  if (loaded.has(id)) return;
  loaded.add(id);
  const link = document.createElement('link');
  link.rel = 'stylesheet';
  link.href = href;
  document.head.appendChild(link);
}

/** Loads the Material Symbols families needed (outlined/rounded/sharp) and the text font. */
export function loadFonts(iconFamilies, font = appearance.font) {
  for (const f of iconFamilies) {
    const family = `Material+Symbols+${f[0].toUpperCase()}${f.slice(1)}`;
    addStylesheet(`icons-${f}`, `https://fonts.googleapis.com/css2?family=${family}:opsz,wght,FILL,GRAD@24,500,0..1,0&icon_names=${ICON_NAMES.join(',')}&display=block`);
  }
  if (font === 'serif') addStylesheet('font-serif', 'https://fonts.googleapis.com/css2?family=Roboto+Serif:opsz,wght@8..144,400..900&display=swap');
  if (font === 'mono') addStylesheet('font-mono', 'https://fonts.googleapis.com/css2?family=Roboto+Mono:wght@400..700&display=swap');
}
