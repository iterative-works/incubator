package works.iterative.incubator.ui.preview

import zio.*

/** Environment type for the Preview Server. Empty for now as we don't need any specific services
  * for the preview server.
  */
type PreviewEnv = Any

/** Task type for the Preview Server.
  */
type PreviewTask[A] = RIO[PreviewEnv, A]
