import React, { useEffect, useRef, useState } from 'react';
import { Bold, Italic, List, ListOrdered, Heading, Quote, Sparkles, RemoveFormatting, Undo2, Redo2, RotateCcw } from 'lucide-react';
import { formatMarkdownToHtml } from '../utils/formatUtils';

export default function ProposalSectionEditor({ block, index, onChange, onRegenerate }) {
  const editorRef = useRef(null);
  const [counts, setCounts] = useState({ words: 0, chars: 0 });
  const [initialContent, setInitialContent] = useState('');

  // Sync content with contentEditable div safely without losing user's active cursor
  useEffect(() => {
    if (editorRef.current) {
      const initialHtml = formatMarkdownToHtml(block.content || '');
      // Only set if HTML is meaningfully different (initial load or external reset)
      if (editorRef.current.innerHTML !== initialHtml && document.activeElement !== editorRef.current) {
        editorRef.current.innerHTML = initialHtml;
        if (!initialContent) {
          setInitialContent(initialHtml);
        }
      }
      updateCounts();
    }
  }, [block.content]);

  const updateCounts = () => {
    if (editorRef.current) {
      const text = editorRef.current.innerText || '';
      const words = text.trim().split(/\s+/).filter(Boolean).length;
      setCounts({ words, chars: text.length });
    }
  };

  const handleInput = () => {
    if (editorRef.current) {
      const newHtml = editorRef.current.innerHTML;
      onChange(newHtml);
      updateCounts();
    }
  };

  const executeCommand = (command, value = null) => {
    if (editorRef.current) {
      editorRef.current.focus();
      document.execCommand(command, false, value);
      handleInput();
    }
  };

  const handleKeyDown = (e) => {
    if (e.ctrlKey || e.metaKey) {
      if (e.key === 'z' || e.key === 'Z') {
        if (e.shiftKey) {
          e.preventDefault();
          executeCommand('redo');
        } else {
          e.preventDefault();
          executeCommand('undo');
        }
      } else if (e.key === 'y' || e.key === 'Y') {
        e.preventDefault();
        executeCommand('redo');
      } else if (e.key === 'b' || e.key === 'B') {
        e.preventDefault();
        executeCommand('bold');
      } else if (e.key === 'i' || e.key === 'I') {
        e.preventDefault();
        executeCommand('italic');
      }
    }
  };

  const handleRevert = () => {
    if (editorRef.current && initialContent) {
      editorRef.current.innerHTML = initialContent;
      handleInput();
    }
  };

  return (
    <section className="bg-surface border border-border rounded-lg overflow-hidden shadow-xs hover:border-brass/50 transition-all">
      {/* Block Header with Title, Index, and Section Actions */}
      <div className="bg-paper/80 border-b border-border px-4 py-2.5 flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="font-mono text-xs font-bold text-brass-dark bg-brass-soft/40 px-1.5 py-0.5 rounded">
            {String(index + 1).padStart(2, '0')}
          </span>
          <h3 className="font-display font-bold text-sm text-ink">{block.title}</h3>
        </div>

        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={handleRevert}
            title="Revert all edits in this section back to original"
            className="text-[11px] text-ink-muted hover:text-rust flex items-center gap-1 px-2 py-0.5 rounded hover:bg-paper cursor-pointer font-medium transition-colors"
          >
            <RotateCcw className="w-3 h-3 text-rust" />
            <span>Revert</span>
          </button>

          {onRegenerate && (
            <button
              type="button"
              onClick={() => onRegenerate(block, index)}
              title="Regenerate this section"
              className="text-[11px] text-ink-muted hover:text-brass-dark flex items-center gap-1 px-2 py-0.5 rounded hover:bg-paper cursor-pointer font-medium"
            >
              <Sparkles className="w-3 h-3 text-brass-dark" />
              <span>Regen</span>
            </button>
          )}
        </div>
      </div>

      {/* WYSIWYG Formatting & Undo/Redo Toolbar */}
      <div className="px-3 py-1.5 bg-paper/30 border-b border-border/60 flex flex-wrap items-center gap-1 text-ink-muted text-xs">
        {/* Undo and Redo controls */}
        <button
          type="button"
          onMouseDown={(e) => { e.preventDefault(); executeCommand('undo'); }}
          className="p-1.5 rounded hover:bg-paper hover:text-ink flex items-center justify-center transition-colors cursor-pointer"
          title="Undo (Ctrl+Z)"
        >
          <Undo2 className="w-3.5 h-3.5" />
        </button>

        <button
          type="button"
          onMouseDown={(e) => { e.preventDefault(); executeCommand('redo'); }}
          className="p-1.5 rounded hover:bg-paper hover:text-ink flex items-center justify-center transition-colors cursor-pointer"
          title="Redo (Ctrl+Y)"
        >
          <Redo2 className="w-3.5 h-3.5" />
        </button>

        <span className="w-[1px] h-4 bg-border mx-1" />

        {/* Text styling */}
        <button
          type="button"
          onMouseDown={(e) => { e.preventDefault(); executeCommand('bold'); }}
          className="p-1.5 rounded hover:bg-paper hover:text-ink flex items-center justify-center font-bold transition-colors cursor-pointer"
          title="Bold (Ctrl+B)"
        >
          <Bold className="w-3.5 h-3.5" />
        </button>

        <button
          type="button"
          onMouseDown={(e) => { e.preventDefault(); executeCommand('italic'); }}
          className="p-1.5 rounded hover:bg-paper hover:text-ink flex items-center justify-center italic transition-colors cursor-pointer"
          title="Italic (Ctrl+I)"
        >
          <Italic className="w-3.5 h-3.5" />
        </button>

        <span className="w-[1px] h-4 bg-border mx-1" />

        <button
          type="button"
          onMouseDown={(e) => { e.preventDefault(); executeCommand('formatBlock', '<h4>'); }}
          className="px-1.5 py-1 rounded hover:bg-paper hover:text-ink font-display font-bold text-xs transition-colors flex items-center gap-0.5 cursor-pointer"
          title="Heading"
        >
          <Heading className="w-3.5 h-3.5" />
        </button>

        <button
          type="button"
          onMouseDown={(e) => { e.preventDefault(); executeCommand('insertUnorderedList'); }}
          className="p-1.5 rounded hover:bg-paper hover:text-ink flex items-center justify-center transition-colors cursor-pointer"
          title="Bullet List"
        >
          <List className="w-3.5 h-3.5" />
        </button>

        <button
          type="button"
          onMouseDown={(e) => { e.preventDefault(); executeCommand('insertOrderedList'); }}
          className="p-1.5 rounded hover:bg-paper hover:text-ink flex items-center justify-center transition-colors cursor-pointer"
          title="Numbered List"
        >
          <ListOrdered className="w-3.5 h-3.5" />
        </button>

        <button
          type="button"
          onMouseDown={(e) => { e.preventDefault(); executeCommand('formatBlock', '<blockquote>'); }}
          className="p-1.5 rounded hover:bg-paper hover:text-ink flex items-center justify-center transition-colors cursor-pointer"
          title="Quote / Callout Box"
        >
          <Quote className="w-3.5 h-3.5" />
        </button>

        <button
          type="button"
          onMouseDown={(e) => { e.preventDefault(); executeCommand('removeFormat'); }}
          className="p-1.5 rounded hover:bg-paper hover:text-ink flex items-center justify-center transition-colors cursor-pointer"
          title="Clear formatting"
        >
          <RemoveFormatting className="w-3.5 h-3.5" />
        </button>

        <div className="ml-auto text-[10px] font-mono text-ink-faint flex items-center gap-2">
          <span>{counts.words} words</span>
          <span>•</span>
          <span>{counts.chars} chars</span>
        </div>
      </div>

      {/* True Inline WYSIWYG Editable Surface */}
      <div className="p-4 bg-surface">
        <div
          ref={editorRef}
          contentEditable
          suppressContentEditableWarning
          onInput={handleInput}
          onBlur={handleInput}
          onKeyDown={handleKeyDown}
          data-placeholder={`Type or format ${block.title}...`}
          className="wysiwyg-editor min-h-[130px] text-ink text-sm font-sans leading-relaxed focus:outline-none"
        />
      </div>
    </section>
  );
}
