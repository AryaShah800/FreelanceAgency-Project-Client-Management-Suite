/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        paper: '#F7F4EE',
        surface: '#FFFFFF',
        ink: {
          DEFAULT: '#1C1B19',
          muted: '#6B6862',
          faint: '#A39E93'
        },
        brass: {
          DEFAULT: '#B8860B',
          soft: '#E8D9B5',
          dark: '#8C6508'
        },
        forest: {
          DEFAULT: '#1F4B43',
          soft: '#DCE8E5'
        },
        rust: {
          DEFAULT: '#A6432B',
          soft: '#F0DCD5'
        },
        border: '#E4DFD3',
      },
      fontFamily: {
        display: ['"Fraunces"', 'serif'],
        sans: ['"Space Grotesk"', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
    },
  },
  plugins: [],
}
