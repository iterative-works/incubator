import "stylesheets/main.css";
import htmx from "htmx.org";

import { setBasePath } from "@shoelace-style/shoelace/dist/utilities/base-path.js";

// Make htmx available globally
window.htmx = htmx;

setBasePath("/assets/shoelace");
