import { defineConfig } from "vite";
import { viteStaticCopy } from "vite-plugin-static-copy";
import tailwindcss from "@tailwindcss/vite";
import path from "path";

const appPath = ".";
const modelPath = ".";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  return {
    publicDir: `${appPath}/src/main/static/public`,
    resolve: {
      alias: {
        resources: path.resolve(__dirname, `${appPath}/src/main/resources`),
        stylesheets: path.resolve(
          __dirname,
          `${appPath}/src/main/static/stylesheets`,
        ),
      },
    },
    // base: "/front/",
    build: {
      manifest: true,
      rollupOptions: {
        input: {
          main: path.resolve(__dirname, "main.js"),
        },
      },
      outDir: "./target/vite",
    },
    plugins: [
      tailwindcss(),
      viteStaticCopy({
        targets: [
          {
            src: "node_modules/@shoelace-style/shoelace/dist/assets/*",
            dest: "assets/shoelace/assets",
          },
        ],
      }),
    ],
  };
});
