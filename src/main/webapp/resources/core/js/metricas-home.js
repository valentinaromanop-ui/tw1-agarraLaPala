document.querySelectorAll("[data-count]").forEach((counter) => {
  const target = Number(counter.dataset.count);
  if (!Number.isFinite(target)) {
    return;
  }

  const suffix = counter.dataset.countSuffix || "";
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    counter.textContent = `${target}${suffix}`;
    return;
  }

  const start = performance.now();
  const duration = 1200;
  const updateCount = (now) => {
    const progress = Math.min((now - start) / duration, 1);
    counter.textContent = `${Math.floor(target * progress)}${suffix}`;
    if (progress < 1) {
      window.requestAnimationFrame(updateCount);
    }
  };
  window.requestAnimationFrame(updateCount);
});
