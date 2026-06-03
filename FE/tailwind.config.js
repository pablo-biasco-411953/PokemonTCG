/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  corePlugins: {
    preflight: false,
  },
  theme: {
    extend: {
      fontFamily: {
        display: ['Rajdhani', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        holo: '0 20px 70px rgba(14, 165, 233, .22), 0 0 42px rgba(250, 204, 21, .18)',
      },
    },
  },
  plugins: [],
};
