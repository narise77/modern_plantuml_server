/*************
* Preview JS *
**************/

async function initPreview(view) {
  const editorContainer = document.getElementById("editor-main-container");
  const previewContainer = document.getElementById("previewer-main-container");

  function hidePreview() {
    if (editorContainer) editorContainer.style.width = "100%";
    if (previewContainer) setVisibility(previewContainer, false);
  }
  // init preview components
  const hasDiagramOptions = initDiagramOptions();
  if (hasDiagramOptions && document.appConfig.diagramPreviewType === "png") {
    // the preloaded png was rendered without the diagram options
    await setDiagram("png", document.appData.encodedDiagram, document.appData.index);
  } else {
    await initializeDiagram();
  }
  initializePaginator()
  // check preview visibility
  if (["previewer", "editor"].includes(view)) {
    hidePreview();
  }
}
