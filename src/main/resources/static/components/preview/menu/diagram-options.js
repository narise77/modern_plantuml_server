/**********************
* Diagram Options JS *
***********************
* - Font: written into the monaco editor code as `skinparam defaultFontName`.
* - DPI: written into the monaco editor code as comment (e.g. `'150dpi`) before the closing tag.
***********************/

const { initDiagramOptions } = (function() {
  const FONT_LINE = /^[ \t]*skinparam[ \t]+defaultFontName[ \t]+.*(\r?\n|$)/im;
  const FONT_VALUE = /^[ \t]*skinparam[ \t]+defaultFontName[ \t]+"?([^"\r\n]*)"?/im;
  const DPI_LINE = /^[ \t]*'[ \t]*\d{2,3}[ \t]*dpi[ \t]*(\r?\n|$)/im;
  const DPI_VALUE = /^[ \t]*'[ \t]*(\d{2,3})[ \t]*dpi\b/im;

  const END_TAG_PATTERN = /^[ \t]*@end\w*.*$/im;

  /************* font (editor code) *************/
  function getFontFromCode(code) {
    const match = FONT_VALUE.exec(code || "");
    return match ? match[1].trim() : "";
  }
  function applyFontToCode(code, font) {
    if (!font) return code.replace(FONT_LINE, "");
    const line = 'skinparam defaultFontName "' + font + '"';
    if (FONT_LINE.test(code)) return code.replace(FONT_LINE, line + "$1");
    // insert right after the first @startXXX line (or at the top)
    const start = /^[ \t]*@start\w*.*(\r?\n|$)/im.exec(code);
    if (!start) return line + "\n" + code;
    const pos = start.index + start[0].length;
    return code.slice(0, pos) + (start[1] ? "" : "\n") + line + "\n" + code.slice(pos);
  }
  function initFontSelect() {
    const select = document.getElementById("font-select");
    if (!select || !document.editor) return;
    select.addEventListener("change", () => {
      const code = document.editor.getValue();
      const newCode = applyFontToCode(code, select.value);
      // editor change triggers the diagram refresh
      if (newCode !== code) setEditorValue(document.editor, newCode);
    });
    // keep the dropdown in sync with the editor code
    const sync = () => {
      const font = getFontFromCode(document.editor.getValue());
      const nextVal = [...select.options].some(o => o.value === font) ? font : "";
      if (select.value !== nextVal) select.value = nextVal;
    };
    sync();
    document.editor.onDidChangeModelContent(sync);
  }

  /************* dpi (editor code) *************/
  function getDpiFromCode(code) {
    const match = DPI_VALUE.exec(code || "");
    return match ? match[1].trim() : "";
  }
  function applyDpiToCode(code, dpi) {
    if (!dpi) {
      return code.replace(DPI_LINE, "");
    }
    const line = "'" + dpi + "dpi";
    if (DPI_LINE.test(code)) {
      return code.replace(DPI_LINE, function(match, p1) {
        return line + (p1 || "\n");
      });
    }
    // insert right before the closing tag (@endgantt, @enduml, @endmindmap, etc.)
    const endMatch = END_TAG_PATTERN.exec(code);
    const newline = /\r\n/.test(code) ? "\r\n" : "\n";
    if (endMatch) {
      const pos = endMatch.index;
      const before = code.slice(0, pos);
      const after = code.slice(pos);
      const needPrefixNewline = before.length > 0 && !before.endsWith("\n") && !before.endsWith("\r");
      return before + (needPrefixNewline ? newline : "") + line + newline + after;
    }
    // fallback: insert at the end if no @end tag
    const trimmed = code.trimEnd();
    return (trimmed.length > 0 ? trimmed + newline : "") + line + newline;
  }
  function initDpiSelect() {
    const select = document.getElementById("dpi-select");
    if (!select || !document.editor) return;
    try {
      localStorage.removeItem("document.diagramOptions");
    } catch (e) {}
    select.addEventListener("change", () => {
      const code = document.editor.getValue();
      const newCode = applyDpiToCode(code, select.value);
      // editor change triggers the diagram refresh
      if (newCode !== code) setEditorValue(document.editor, newCode);
    });
    // keep the dropdown in sync with the editor code
    const sync = () => {
      const dpi = getDpiFromCode(document.editor.getValue());
      const nextVal = [...select.options].some(o => o.value === dpi) ? dpi : "";
      if (select.value !== nextVal) select.value = nextVal;
    };
    sync();
    document.editor.onDidChangeModelContent(sync);
  }

  /**
   * Initialize the diagram options.
   *
   * @returns {boolean} true if a diagram-only option is set, i.e. the preloaded diagram has to be refreshed
   */
  function initDiagramOptions() {
    document.diagramOptions = {};
    initFontSelect();
    initDpiSelect();
    return Object.values(document.diagramOptions).some(v => v);
  }

  return { initDiagramOptions };
})();
