// Babel is used only by Jest (babel-jest) to transform JSX/ESM in tests; Vite uses esbuild.
module.exports = {
  presets: [
    ['@babel/preset-env', { targets: { node: 'current' } }],
    ['@babel/preset-react', { runtime: 'automatic' }],
  ],
};
