import DOMPurify from 'dompurify';

export function sanitizeHtml(input = '') {
  return DOMPurify.sanitize(input, {
    USE_PROFILES: { html: true },
    FORBID_TAGS: ['iframe', 'object', 'embed', 'form', 'style', 'link', 'meta'],
    FORBID_ATTR: ['style'],
  });
}

/**
 * Safely parses markdown or plain text into clean HTML if the input is not already HTML.
 */
export function formatMarkdownToHtml(input = '') {
  if (!input) return '';

  // If already formatted HTML containing structural tags
  if (/<(p|ul|ol|li|h[1-6]|blockquote|strong|em|div|span)[^>]*>/i.test(input)) {
    return sanitizeHtml(input);
  }

  const lines = input.split('\n');
  const renderedElements = [];
  let inBulletList = false;
  let inNumberedList = false;
  let currentListItems = [];

  const flushList = () => {
    if (inBulletList && currentListItems.length > 0) {
      renderedElements.push(
        `<ul>${currentListItems.map(item => `<li>${item}</li>`).join('')}</ul>`
      );
      currentListItems = [];
      inBulletList = false;
    }
    if (inNumberedList && currentListItems.length > 0) {
      renderedElements.push(
        `<ol>${currentListItems.map(item => `<li>${item}</li>`).join('')}</ol>`
      );
      currentListItems = [];
      inNumberedList = false;
    }
  };

  const parseInline = (text) => {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/`([^`]+)`/g, '<code class="bg-border/60 px-1 py-0.5 rounded text-xs font-mono text-brass-dark">$1</code>');
  };

  for (let i = 0; i < lines.length; i++) {
    const rawLine = lines[i];
    const trimmed = rawLine.trim();

    if (!trimmed) {
      flushList();
      continue;
    }

    // Heading (e.g. ### Subheading)
    if (trimmed.startsWith('### ')) {
      flushList();
      renderedElements.push(`<h4>${parseInline(trimmed.substring(4))}</h4>`);
      continue;
    }

    // Blockquote
    if (trimmed.startsWith('> ')) {
      flushList();
      renderedElements.push(`<blockquote>${parseInline(trimmed.substring(2))}</blockquote>`);
      continue;
    }

    // Bullet list (• or - or *)
    const bulletMatch = trimmed.match(/^([•\-\*])\s+(.+)$/);
    if (bulletMatch) {
      if (inNumberedList) flushList();
      inBulletList = true;
      currentListItems.push(parseInline(bulletMatch[2]));
      continue;
    }

    // Numbered list (1. or 2.)
    const numberMatch = trimmed.match(/^(\d+[\.\)])\s+(.+)$/);
    if (numberMatch) {
      if (inBulletList) flushList();
      inNumberedList = true;
      currentListItems.push(parseInline(numberMatch[2]));
      continue;
    }

    flushList();
    renderedElements.push(`<p>${parseInline(trimmed)}</p>`);
  }

  flushList();
  return sanitizeHtml(renderedElements.join(''));
}
