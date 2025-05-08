package works.iterative.incubator.budget.ui.transaction_import.styles

import scalatags.Text.all.*
import scalatags.Text.tags2

/** Custom styles for HTMX-related functionality Provides CSS for HTMX indicators
  *
  * Category: UI Styles Layer: UI/Presentation
  */
object HtmxStyles:
    /** Gets a Scalatags fragment with CSS styles for HTMX indicators
      *
      * @return
      *   A style tag with HTMX indicator styles
      */
    def getStyles: Frag =
        tags2.style(raw("""
          /* Hide elements with htmx-indicator class when not in htmx request */
          .htmx-indicator {
            opacity: 0;
            transition: opacity 200ms ease-in;
          }

          /* Show htmx-indicator elements during request */
          .htmx-request .htmx-indicator {
            opacity: 1;
            display: inline-block !important;
          }

          /* Hide elements with htmx-indicator-inverse class during request */
          .htmx-request .htmx-indicator-inverse {
            display: none;
          }

          /* Attributes for button state during requests */
          .htmx-request.btn,
          .htmx-request.btn:hover {
            cursor: progress;
          }

          /* Prevent interaction during requests */
          .htmx-request.form {
            cursor: progress;
          }

          /* Style for the form when request is in progress */
          .htmx-request input,
          .htmx-request select,
          .htmx-request textarea {
            cursor: progress;
          }
        """))
end HtmxStyles
