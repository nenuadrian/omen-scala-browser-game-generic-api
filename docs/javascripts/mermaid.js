document$.subscribe(function () {
  if (typeof mermaid !== "undefined") {
    mermaid.initialize({ startOnLoad: false });
    mermaid.run();
  }
});
