/********************
* Editor Sample JS  *
********************/

const { initEditorSample } = (function() {
  function getSampleCodeFromOption(option) {
    if (!option) return "";
    return option.getAttribute("data-code") || "";
  }

  function getTitleFromCode(code) {
    if (!code) return "";
    const match = /^[ \t]*title(?:\s+|:\s*)(?:"([^"\r\n]+)"|'([^'\r\n]+)'|([^\r\n]+))/im.exec(code);
    if (match) {
      let t = (match[1] || match[2] || match[3] || "").trim();
      if (t.startsWith('"') && t.endsWith('"') && t.length >= 2) {
        t = t.slice(1, -1).trim();
      }
      return t;
    }
    return "";
  }

  function initEditorSample() {
    const select = document.getElementById("sample-select");
    if (!select) return;

    // Fallback: If options were not pre-rendered by Thymeleaf, load via ui-helper
    if (select.options.length <= 1) {
      fetch("ui-helper?request=samples")
        .then(function(res) {
          if (res.ok) {
            return res.json();
          }
          throw new Error("Failed to fetch samples");
        })
        .then(function(samples) {
          if (Array.isArray(samples) && samples.length > 0) {
            samples.forEach(function(sample) {
              const opt = document.createElement("option");
              opt.value = sample.filename || "";
              opt.textContent = getTitleFromCode(sample.code) || sample.name || "";
              opt.setAttribute("data-code", sample.code || "");
              select.appendChild(opt);
            });
            syncSelectWithEditor(select);
          }
        })
        .catch(function(e) {
          // ignore fallback fetch error
        });
    }

    function syncSelectWithEditor(sel) {
      if (!sel || !document.editor) return;
      const currentCode = document.editor.getValue();
      const currentTitle = getTitleFromCode(currentCode);
      let matchedIndex = -1;
      for (let i = 1; i < sel.options.length; i++) {
        const code = getSampleCodeFromOption(sel.options[i]);
        if (code && code.trim() === currentCode.trim()) {
          matchedIndex = i;
          break;
        }
      }
      if (matchedIndex < 0 && currentTitle) {
        for (let i = 1; i < sel.options.length; i++) {
          const opt = sel.options[i];
          const optTitle = getTitleFromCode(getSampleCodeFromOption(opt)) || opt.textContent.trim();
          if (optTitle === currentTitle) {
            matchedIndex = i;
            break;
          }
        }
      }
      if (matchedIndex >= 0) {
        sel.selectedIndex = matchedIndex;
      } else {
        sel.value = "";
      }
    }

    select.addEventListener("change", function() {
      const selectedOption = select.options[select.selectedIndex];
      if (!selectedOption || !selectedOption.value) return;

      const code = getSampleCodeFromOption(selectedOption);
      if (code && document.editor) {
        setEditorValue(document.editor, code);
        document.editor.focus();
      } else if (selectedOption.value) {
        // Fallback fetch from static/sample/filename
        fetch("sample/" + encodeURIComponent(selectedOption.value))
          .then(function(res) { return res.text(); })
          .then(function(fetchedCode) {
            selectedOption.setAttribute("data-code", fetchedCode);
            if (document.editor) {
              setEditorValue(document.editor, fetchedCode);
              document.editor.focus();
            }
          })
          .catch(function(e) {
            // ignore fetch error
          });
      }
    });

    if (document.editor) {
      syncSelectWithEditor(select);
      document.editor.onDidChangeModelContent(function() {
        syncSelectWithEditor(select);
      });
    }
  }

  return {
    initEditorSample: initEditorSample,
  };
})();

document.initEditorSample = initEditorSample;
