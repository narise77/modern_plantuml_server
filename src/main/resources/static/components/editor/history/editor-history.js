/********************
* Editor History JS *
*********************/

const {
  initEditorHistory,
  saveEditorHistory,
  getEditorHistory,
  clearEditorHistory,
} = (function() {
  const MAX_HISTORY_ITEMS = 20;
  const STORAGE_KEYS = [
    "document.inputHistory",
    "plantuml.inputHistory",
    "inputHistory"
  ];

  function loadHistoryFromStorage() {
    for (let i = 0; i < STORAGE_KEYS.length; i++) {
      try {
        const raw = localStorage.getItem(STORAGE_KEYS[i]);
        if (raw) {
          const parsed = JSON.parse(raw);
          if (Array.isArray(parsed)) {
            return parsed
              .map(function(item) {
                return typeof item === "string" ? item : (item && item.text ? item.text : "");
              })
              .filter(function(text) {
                return typeof text === "string" && text.trim().length > 0;
              })
              .slice(0, MAX_HISTORY_ITEMS);
          }
        }
      } catch (e) {
        // ignore parse errors
      }
    }
    return [];
  }

  function writeHistoryToStorage(history) {
    const json = JSON.stringify(history.slice(0, MAX_HISTORY_ITEMS));
    STORAGE_KEYS.forEach(function(key) {
      try {
        localStorage.setItem(key, json);
      } catch (e) {
        // ignore storage errors
      }
    });
  }

  function getPreviewLabel(text, maxLength) {
    if (!text) return "";
    maxLength = maxLength || 30;
    const lines = text.split(/\r?\n/).map(function(l) { return l.trim(); }).filter(Boolean);
    if (lines.length === 0) return "";
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      if (/^title\s+/i.test(line)) {
        let titleText = line.replace(/^title\s+/i, "").replace(/'/g, "").trim();
        if (titleText.length > 0) {
          return titleText.length > maxLength ? titleText.slice(0, maxLength) + "..." : titleText;
        }
      }
    }
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      if (!/^@start/i.test(line) && !/^@end/i.test(line)) {
        if (/^'?\s*\d{2,3}\s*dpi\b/i.test(line)) continue;
        let clean = line.replace(/^'+/g, "").replace(/'/g, "").trim();
        if (clean.length > 0) {
          return clean.length > maxLength ? clean.slice(0, maxLength) + "..." : clean;
        }
      }
    }
    const firstLine = lines[0].replace(/'/g, "").trim();
    return firstLine.length > maxLength ? firstLine.slice(0, maxLength) + "..." : firstLine;
  }

  function renderHistorySelectOptions(history) {
    const select = document.getElementById("history-select");
    if (!select) return;

    const previousValue = select.value;
    select.innerHTML = "";

    const defaultOption = document.createElement("option");
    defaultOption.value = "";
    defaultOption.disabled = true;
    defaultOption.selected = true;
    defaultOption.textContent = history.length > 0
      ? "履歴から自動完成 (" + history.length + "/20)"
      : "履歴なし (最大20世代)";
    select.appendChild(defaultOption);

    history.forEach(function(text, index) {
      const opt = document.createElement("option");
      opt.value = String(index);
      const summary = getPreviewLabel(text, 28).replace(/'/g, "").trim();
      opt.textContent = (index + 1) + ": " + summary;
      opt.title = text.slice(0, 100);
      select.appendChild(opt);
    });

    if (previousValue !== "" && !isNaN(parseInt(previousValue, 10))) {
      const idx = parseInt(previousValue, 10);
      if (idx < history.length && document.editor && document.editor.getValue() === history[idx]) {
        select.value = String(idx);
      }
    }
  }

  function getEditorHistory() {
    return loadHistoryFromStorage();
  }

  function clearEditorHistory() {
    STORAGE_KEYS.forEach(function(key) {
      try {
        localStorage.removeItem(key);
      } catch (e) {}
    });
    renderHistorySelectOptions([]);
  }

  function saveEditorHistory(code) {
    if (!code || typeof code !== "string" || !code.trim()) return;
    let history = loadHistoryFromStorage();
    if (history.length > 0 && history[0] === code) {
      return;
    }
    history = history.filter(function(item) {
      return item !== code;
    });
    history.unshift(code);
    if (history.length > MAX_HISTORY_ITEMS) {
      history = history.slice(0, MAX_HISTORY_ITEMS);
    }
    writeHistoryToStorage(history);
    renderHistorySelectOptions(history);
  }

  function registerMonacoHistoryCompletion() {
    if (!window.monaco || !monaco.languages || !monaco.languages.registerCompletionItemProvider) {
      return;
    }
    try {
      monaco.languages.registerCompletionItemProvider(["apex", "plantuml"], {
        provideCompletionItems: function(model, position) {
          const history = loadHistoryFromStorage();
          if (!history || history.length === 0) {
            return { suggestions: [] };
          }
          const fullRange = model.getFullModelRange();
          const suggestions = history.map(function(text, idx) {
            const preview = getPreviewLabel(text, 25).replace(/'/g, "").trim();
            return {
              label: {
                label: "履歴 #" + (idx + 1) + ": " + preview,
                description: "第" + (idx + 1) + "世代",
              },
              kind: monaco.languages.CompletionItemKind.Snippet,
              detail: "入力テキスト履歴 (第" + (idx + 1) + "世代)",
              documentation: text,
              insertText: text,
              range: fullRange,
              sortText: "0000_history_" + (idx < 10 ? "0" + idx : idx),
            };
          });
          return { suggestions: suggestions };
        }
      });
    } catch (e) {
      // ignore registration error
    }
  }

  function initEditorHistory() {
    const select = document.getElementById("history-select");
    const history = loadHistoryFromStorage();

    if (document.editor) {
      const initialCode = document.editor.getValue();
      if (initialCode && initialCode.trim()) {
        if (history.length === 0) {
          saveEditorHistory(initialCode);
        }
      }
    }

    renderHistorySelectOptions(loadHistoryFromStorage());

    if (select) {
      select.addEventListener("change", function() {
        const val = select.value;
        if (val === "" || isNaN(parseInt(val, 10))) return;
        const index = parseInt(val, 10);
        const currentHistory = loadHistoryFromStorage();
        if (index >= 0 && index < currentHistory.length && document.editor) {
          const chosenText = currentHistory[index];
          setEditorValue(document.editor, chosenText);
          document.editor.focus();
        }
      });
    }

    if (document.editor) {
      document.editor.onDidChangeModelContent(function() {
        if (!select) return;
        const currentCode = document.editor.getValue();
        const currentHistory = loadHistoryFromStorage();
        const foundIdx = currentHistory.indexOf(currentCode);
        if (foundIdx >= 0) {
          select.value = String(foundIdx);
        } else {
          select.value = "";
        }
      });
    }

    window.addEventListener("beforeunload", function() {
      if (document.editor) {
        saveEditorHistory(document.editor.getValue());
      }
    });

    registerMonacoHistoryCompletion();
  }

  // Alias lookup for document.getElementById so both IDs can be queried
  const origGetElementById = document.getElementById.bind(document);
  document.getElementById = function(id) {
    if (id === "editor-history-select" || id === "input-history-select") {
      return origGetElementById("history-select") || origGetElementById(id);
    }
    return origGetElementById(id);
  };

  return {
    initEditorHistory: initEditorHistory,
    saveEditorHistory: saveEditorHistory,
    getEditorHistory: getEditorHistory,
    clearEditorHistory: clearEditorHistory,
  };
})();

document.initEditorHistory = initEditorHistory;
document.saveEditorHistory = saveEditorHistory;
document.getEditorHistory = getEditorHistory;
document.clearEditorHistory = clearEditorHistory;
