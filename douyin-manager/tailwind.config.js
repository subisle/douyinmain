/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#1E40AF',
          light: '#3B82F6'
        },
        background: {
          DEFAULT: '#0F172A',
          light: '#1E293B'
        },
        text: {
          DEFAULT: '#F1F5F9',
          muted: '#94A3B8'
        },
        accent: {
          gold: '#F59E0B',
          green: '#10B981',
          blue: '#3B82F6'
        }
      },
      fontFamily: {
        sans: ['PingFang SC', 'Microsoft YaHei', 'sans-serif'],
        mono: ['Roboto Mono', 'Consolas', 'monospace']
      }
    },
  },
  plugins: [],
}
