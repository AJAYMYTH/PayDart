/** @type {import('tailwindcss').Config} */
export default {
  content: ['./src/**/*.{astro,html,js,jsx,md,mdx,svelte,ts,tsx,vue}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        background: '#040805',
        surface: {
          50: '#08120b',
          100: '#0d1d12',
          200: '#142b1b',
          300: '#1c3c26',
        },
        brand: {
          primary: '#00F076',       // Vibrant Cyber Emerald
          neon: '#10B981',          // Premium Emerald 500
          light: '#6EE7B7',         // Mint 300
          dark: '#064E3B',          // Deep Emerald 900
          glow: 'rgba(0, 240, 118, 0.45)',
        },
        white: '#FFFFFF',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
        display: ['Cabinet Grotesk', 'Inter', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        'glow-sm': '0 0 15px rgba(0, 240, 118, 0.25)',
        'glow-md': '0 0 30px rgba(0, 240, 118, 0.35)',
        'glow-lg': '0 0 60px rgba(0, 240, 118, 0.45)',
        'glow-white': '0 0 35px rgba(255, 255, 255, 0.25)',
      },
      keyframes: {
        'radar-sweep': {
          '0%': { transform: 'rotate(0deg)' },
          '100%': { transform: 'rotate(360deg)' },
        },
        'scan-line': {
          '0%, 100%': { transform: 'translateY(-100%)' },
          '50%': { transform: 'translateY(100%)' },
        },
        'pulse-slow': {
          '0%, 100%': { opacity: '0.4', transform: 'scale(1)' },
          '50%': { opacity: '0.8', transform: 'scale(1.05)' },
        },
        'float': {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-10px)' },
        },
      },
      animation: {
        'radar-sweep': 'radar-sweep 4s linear infinite',
        'scan-line': 'scan-line 2.5s ease-in-out infinite',
        'pulse-slow': 'pulse-slow 4s ease-in-out infinite',
        'float': 'float 6s ease-in-out infinite',
      },
    },
  },
  plugins: [],
};
