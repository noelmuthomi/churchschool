/**
 * REQUIRED for the iPhone/PWA wrapper (it shows your app inside a frame).
 * Update your existing doGet so it includes setXFrameOptionsMode(ALLOWALL),
 * then: Deploy > Manage deployments > Edit > New version > Deploy.
 * Keep "Who has access" = Anyone (otherwise Google blocks the frame).
 * Replace 'Index' with the name of your HTML file.
 */
function doGet(e) {
  return HtmlService.createTemplateFromFile('Index')
    .evaluate()
    .setTitle('Church School Ministry')
    .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL)
    .addMetaTag('viewport', 'width=device-width, initial-scale=1, viewport-fit=cover');
}
