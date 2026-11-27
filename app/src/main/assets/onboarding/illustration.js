const illustration = document.querySelector('.illustration');
const designSize = getComputedStyle(illustration);
const designWidth = parseFloat(designSize.width);
const designHeight = parseFloat(designSize.height);
const viewport = window.visualViewport;
let nativeWidth = 0;
let nativeHeight = 0;

function fitIllustration() {
  if (!nativeWidth || !nativeHeight) return;
  const pixelsPerCssPixel = devicePixelRatio * (viewport ? viewport.scale : 1);
  const width = nativeWidth / pixelsPerCssPixel;
  const height = nativeHeight / pixelsPerCssPixel;
  const scale = Math.min(width / designWidth, height / designHeight);
  const x = (width - designWidth * scale) / 2;
  const y = (height - designHeight * scale) / 2;
  illustration.style.transform = `translate(${x}px, ${y}px) scale(${scale})`;
  illustration.style.visibility = 'visible';
}

window.setIllustrationViewport = (width, height) => {
  nativeWidth = width;
  nativeHeight = height;
  fitIllustration();
};

addEventListener('resize', fitIllustration);
if (viewport) viewport.addEventListener('resize', fitIllustration);
