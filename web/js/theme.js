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
export const PALETTES = [['violet', 'Violet'], ['ocean', 'Ocean'], ['forest', 'Forest'], ['sunset', 'Sunset'], ['rose', 'Rose'], ['custom', 'Custom']];
export const TABS = [['books', 'Books', 'menu_book', ''], ['genres', 'Genres', 'category', 'genres'], ['library', 'Library', 'local_library', 'library'], ['stats', 'Stats', 'bar_chart', 'stats'], ['settings', 'Settings', 'settings', 'settings']];
export const SECTIONS = [['reading', 'Currently reading'], ['want', 'Want to read'], ['read', 'Read']];

const DEFAULTS = {
  theme: 'system', palette: 'violet', textSize: 'default', font: 'sans', bold: true, corners: 'xround', icons: 'outlined',
  customColor: '#6B2BD9', tabOrder: TABS.map((t) => t[0]), sectionOrder: SECTIONS.map((x) => x[0]), myFonts: [],
};

/** A saved order with any new entries appended, so tabs/sections never disappear. */
// A saved order, with anything added in an update slotted in where it is by default.
const fullOrder = (saved, all) => {
  const out = [...new Set((saved || []).filter((x) => all.includes(x)))];
  if (!out.length) return [...all];
  all.forEach((x, i) => { if (!out.includes(x)) out.splice(Math.min(i, out.length), 0, x); });
  return out;
};

export let appearance = normalize({ ...DEFAULTS, ...(store.get(KEY) || {}) });

function normalize(a) {
  a.tabOrder = fullOrder(a.tabOrder, TABS.map((t) => t[0]));
  a.sectionOrder = fullOrder(a.sectionOrder, SECTIONS.map((x) => x[0]));
  return a;
}

export function setAppearance(patch) {
  // Resetting the look keeps the tab/section order and downloaded fonts.
  appearance = normalize(patch === null
    ? { ...DEFAULTS, tabOrder: appearance.tabOrder, sectionOrder: appearance.sectionOrder, myFonts: appearance.myFonts }
    : { ...appearance, ...patch });
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

/* ---------- palettes from any colour ---------- */

function hexToHsl(hex) {
  const n = parseInt(hex.replace('#', ''), 16);
  const r = ((n >> 16) & 255) / 255, g = ((n >> 8) & 255) / 255, b = (n & 255) / 255;
  const max = Math.max(r, g, b), min = Math.min(r, g, b), l = (max + min) / 2;
  let h = 0, s = 0;
  if (max !== min) {
    const d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    h = max === r ? (g - b) / d + (g < b ? 6 : 0) : max === g ? (b - r) / d + 2 : (r - g) / d + 4;
    h *= 60;
  }
  return [h, s, l];
}
export const hsl = (h, s, l) => `hsl(${((h % 360) + 360) % 360} ${Math.round(s * 100)}% ${Math.round(l * 100)}%)`;

/**
 * Light or dark accents from any seed colour (same recipe as the Android app):
 * primary keeps the seed's hue, secondary and tertiary are neighbouring hues.
 */
function seedAccents(hex, dark) {
  const [h, s0] = hexToHsl(hex);
  const s = s0 < 0.08 ? s0 : Math.min(0.9, Math.max(0.45, s0));
  const accent = (hh, ss) => (dark ? [hsl(hh, ss, 0.8), hsl(hh, ss, 0.2), hsl(hh, ss, 0.3), hsl(hh, ss, 0.9)]
    : [hsl(hh, ss, 0.4), '#FFFFFF', hsl(hh, ss, 0.9), hsl(hh, ss, 0.12)]);
  return [accent(h, s), accent(h - 35, s * 0.85), accent(h + 65, s * 0.85)];
}

/** Primary, secondary and tertiary colours of a palette, for swatches. */
export const paletteSwatch = (p, dark) => (p === 'custom'
  ? seedAccents(appearance.customColor, dark).map((x) => x[0])
  : PALETTE_ACCENTS[p].map((name) => A[name][dark ? 1 : 0][0]));
export const seedSwatch = (hex, dark) => seedAccents(hex, dark).map((x) => x[0]);

const media = matchMedia('(prefers-color-scheme: dark)');
export const isDark = () => (appearance.theme === 'system' ? media.matches : appearance.theme === 'dark');
media.addEventListener('change', () => applyAppearance());

export function applyAppearance() {
  const root = document.documentElement;
  const dark = isDark();
  root.dataset.theme = dark ? 'dark' : 'light';
  root.dataset.bold = appearance.bold ? 'on' : 'off';
  root.dataset.icons = appearance.icons;
  const custom = appearance.palette === 'custom' ? seedAccents(appearance.customColor, dark) : null;
  const [p, s, t] = custom || PALETTE_ACCENTS[appearance.palette] || PALETTE_ACCENTS.violet;
  const set = (prefix, name) => {
    const [main, on, container, onContainer] = Array.isArray(name) ? name : A[name][dark ? 1 : 0];
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
  root.style.setProperty('--font', fontStack(appearance.font));
  document.querySelector('meta[name=theme-color]:not([media])')?.setAttribute('content', dark ? '#151218' : '#FFF8F4');
  loadFonts([appearance.icons === 'filled' ? 'outlined' : appearance.icons], appearance.font);
}

/* ---------- any Google Font ---------- */

export const customFontName = (key) => (String(key).startsWith('gf:') ? key.slice(3) : null);
export const fontLabel = (key) => customFontName(key) || (FONTS.find((x) => x[0] === key) || FONTS[0])[1];
export const fontStack = (key) => {
  const name = customFontName(key);
  return name ? `"${name}", ${FONTS[0][2]}` : (FONTS.find((x) => x[0] === key) || FONTS[0])[2];
};

/** Popular Google Fonts offered as suggestions; any other family name can be searched too. */
export const POPULAR_FONTS = [
  'Abril Fatface', 'Alegreya', 'Alfa Slab One', 'Amatic SC', 'Anton', 'Archivo', 'Arvo', 'Asap', 'Atkinson Hyperlegible', 'Audiowide',
  'Baloo 2', 'Barlow', 'Bebas Neue', 'Bitter', 'Cabin', 'Cardo', 'Caveat', 'Chivo', 'Cinzel', 'Comfortaa', 'Cormorant Garamond',
  'Courier Prime', 'Crimson Text', 'DM Sans', 'DM Serif Display', 'Dancing Script', 'EB Garamond', 'Exo 2', 'Figtree', 'Fira Code',
  'Fira Sans', 'Fredoka', 'Great Vibes', 'Heebo', 'Hind', 'IBM Plex Mono', 'IBM Plex Sans', 'IBM Plex Serif', 'Indie Flower', 'Inter',
  'JetBrains Mono', 'Josefin Sans', 'Kalam', 'Karla', 'Kaushan Script', 'Lato', 'Lexend', 'Lexend Deca', 'Libre Baskerville', 'Lobster',
  'Lora', 'Manrope', 'Marcellus', 'Merriweather', 'Montserrat', 'Mulish', 'Noto Sans', 'Noto Serif', 'Nunito', 'Nunito Sans',
  'Old Standard TT', 'Open Sans', 'Orbitron', 'Oswald', 'Outfit', 'Oxygen', 'PT Serif', 'Pacifico', 'Patrick Hand', 'Permanent Marker',
  'Philosopher', 'Playfair Display', 'Plus Jakarta Sans', 'Poppins', 'Press Start 2P', 'Quicksand', 'Raleway', 'Red Hat Display',
  'Righteous', 'Roboto', 'Roboto Condensed', 'Roboto Mono', 'Roboto Slab', 'Rubik', 'Sacramento', 'Satisfy', 'Shadows Into Light',
  'Signika', 'Sora', 'Source Code Pro', 'Source Serif 4', 'Space Grotesk', 'Space Mono', 'Special Elite', 'Spectral', 'Syne',
  'Titillium Web', 'Ubuntu', 'Urbanist', 'VT323', 'Varela Round', 'Vollkorn', 'Work Sans', 'Zilla Slab',
];

const fontCss = new Map();
/** Loads a Google Font (regular and bold); rejects if the family doesn't exist. */
export async function loadGoogleFont(name) {
  const family = name.trim().replace(/\s+/g, ' ');
  if (fontCss.has(family)) return family;
  const enc = encodeURIComponent(family).replace(/%20/g, '+');
  let css = null;
  for (const url of [`https://fonts.googleapis.com/css2?family=${enc}:wght@400;700&display=swap`, `https://fonts.googleapis.com/css2?family=${enc}&display=swap`]) {
    const res = await fetch(url).catch(() => null);
    if (res?.ok) { css = await res.text(); break; }
    if (!res) throw new Error("Couldn't reach Google Fonts. Check your connection.");
  }
  if (!css) throw new Error(`"${family}" isn't on Google Fonts. Check the spelling.`);
  const style = document.createElement('style');
  style.dataset.font = family;
  style.textContent = css;
  document.head.appendChild(style);
  fontCss.set(family, css);
  await document.fonts?.load(`16px "${family}"`).catch(() => {});
  return family;
}

/* ---------- fonts ---------- */

// Every Material Symbol the app shows (Google Fonts subsets to these; must stay sorted).
export const ICON_NAMES = [
  'add', 'arrow_back', 'auto_awesome', 'auto_stories', 'bar_chart', 'bookmark', 'brightness_auto',
  'bug_report', 'call', 'category', 'check', 'check_circle', 'chevron_right', 'close', 'code', 'colorize',
  'content_copy', 'credit_card', 'dark_mode', 'delete', 'download', 'edit', 'error', 'event', 'expand_less',
  'expand_more', 'font_download', 'format_size', 'history', 'key', 'keyboard_arrow_down', 'keyboard_arrow_up',
  'language', 'library_add', 'light_mode', 'link', 'link_off', 'local_fire_department', 'local_library',
  'lock', 'map', 'menu_book', 'my_location', 'new_releases', 'open_in_new', 'palette', 'person', 'place',
  'psychology', 'refresh', 'restart_alt', 'save', 'schedule', 'search', 'settings', 'shopping_cart',
  'storefront', 'style', 'swap_vert', 'task_alt', 'text_fields', 'upload', 'visibility', 'visibility_off',
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
  const custom = customFontName(font);
  if (custom) loadGoogleFont(custom).catch(() => {});
  if (font === 'serif') addStylesheet('font-serif', 'https://fonts.googleapis.com/css2?family=Roboto+Serif:opsz,wght@8..144,400..900&display=swap');
  if (font === 'mono') addStylesheet('font-mono', 'https://fonts.googleapis.com/css2?family=Roboto+Mono:wght@400..700&display=swap');
}
