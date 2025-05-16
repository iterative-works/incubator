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
      tailwindcss({
        content: ["./src/main/scala/**/*.scala"],
        safelist: [
          { pattern: /^fill-/ },
          { pattern: /^text-/ },
          { pattern: /^bg-/ },
        ],
        theme: {
          extend: {
            colors: {
              "ynab-blue": "#1E88E5",
              "ynab-green": "#2E7D32",
              "ynab-red": "#D32F2F",
              "ynab-gray": "#757575",
            },
            animation: {
              "spin-slow": "spin 3s linear infinite",
            },
          },
        },
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
