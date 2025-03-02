import colors from "tailwindcss/colors";
import typography from "@tailwindcss/typography";
import forms from "@tailwindcss/forms";

/** @type {import('tailwindcss').Config} */
export default {
  content: ["./src/main/scala/**/*.scala"],
  safelist: [{ pattern: /^fill-/ }, { pattern: /^text-/ }, { pattern: /^bg-/ }],
  plugins: [typography, forms],
};
