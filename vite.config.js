import { defineConfig } from "vite";
import { viteStaticCopy } from "vite-plugin-static-copy";
import tailwindcss from "@tailwindcss/vite";
import path from "path";
import colors from "tailwindcss/colors";
import typography from "@tailwindcss/typography";
import forms from "@tailwindcss/forms";

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
        "ynab-importer": path.resolve(
          __dirname,
          `${appPath}/ynab-importer/web/src/main/static`,
        ),
      },
    },
    // base: "/front/",
    build: {
      manifest: true,
      rollupOptions: {
        input: {
          main: path.resolve(__dirname, "main.js"),
          "ynab-importer": path.resolve(
            __dirname,
            `${appPath}/ynab-importer/web/src/main/static/js/main.js`,
          ),
        },
      },
      outDir: "./target/vite",
    },
    plugins: [
      tailwindcss({
        content: ["./src/main/scala/**/*.scala"],
        safelist: [
          { pattern: /^fill-/ },
          { pattern: /^text-/ },
          { pattern: /^bg-/ },
        ],
        plugins: [typography, forms],
      }),
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
