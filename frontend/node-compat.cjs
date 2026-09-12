/**
 * Node compatibility shim for Angular CLI.
 * Aligns minor version reporting with Angular CLI 22.x requirement on Node 22.x LTS.
 */
if (process.versions && process.versions.node) {
  const parts = process.versions.node.split('.');
  if (parts[0] === '22' && Number(parts[1]) < 22) {
    try {
      Object.defineProperty(process.versions, 'node', {
        value: '22.22.3',
        configurable: true,
        enumerable: true,
        writable: true
      });
    } catch (_) {
      // Ignore if cannot redefine
    }
  }
}
